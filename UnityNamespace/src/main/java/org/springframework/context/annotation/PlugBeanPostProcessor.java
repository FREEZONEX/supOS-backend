package org.springframework.context.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.util.*;

@Slf4j
public class PlugBeanPostProcessor {

    public static void post(MetadataReaderFactory metadataReaderFactory, BeanDefinitionRegistry registry,
                            SourceExtractor sourceExtractor, ResourceLoader resourceLoader, Environment environment,
                            BeanNameGenerator importBeanNameGenerator) {
        List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
        String[] candidateNames = registry.getBeanDefinitionNames();

        for (String beanName : candidateNames) {
            BeanDefinition beanDef = registry.getBeanDefinition(beanName);
            if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Bean definition has already been processed as a configuration class: " + beanDef);
                }
            } else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, metadataReaderFactory)) {
                configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
            }
        }

        // Return immediately if no @Configuration classes were found
        if (configCandidates.isEmpty()) {
            return;
        }

        // Parse each @Configuration class
        ProblemReporter problemReporter = new FailFastProblemReporter();
        ConfigurationClassParser parser = new ConfigurationClassParser(
                metadataReaderFactory, problemReporter, environment,
                resourceLoader, AnnotationBeanNameGenerator.INSTANCE, registry);

        Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
        Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
        ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;
        final ClassLoader localLoader = PlugBeanPostProcessor.class.getClassLoader();
        do {
            StartupStep processConfig = applicationStartup.start("spring.context.config-classes.parse");
            parser.parse(candidates);
            parser.validate();
            Set<ConfigurationClass> configurationClasses = parser.getConfigurationClasses();
            Set<ConfigurationClass> configClasses = new LinkedHashSet<>(configurationClasses);
            configClasses.removeAll(alreadyParsed);
            configClasses.removeIf(configurationClass -> {
                String beanName = configurationClass.getBeanName();
                boolean del = false;
                Resource resource = configurationClass.getResource();
                if (beanName != null) {
                    if (resource instanceof DescriptiveResource ds) {
                        String className = ds.getDescription();
                        try {
                            localLoader.loadClass(className);
                            del = true;
                        } catch (ClassNotFoundException ex) {
                        }
                    } else {
                        del = registry.containsBeanDefinition(beanName);
                    }
                } else if (resource instanceof ClassPathResource classPathResource) {
                    String path = classPathResource.getPath();
                    del = localLoader.getResource(path) != null;
                } else {
                    if (!resource.exists()) {
                        del = true;
                    } else {
                        String url = null;
                        try {
                            url = resource.getURL().getPath();
                        } catch (Exception ex) {
                        }
                        if (url != null) {
                            int jarPos = url.lastIndexOf(".jar!/");
                            if (jarPos > 0) {
                                String uri = url.substring(jarPos + 6);
                                del = localLoader.getResource(uri) != null;
                            }
                        }
                    }
                }
                return del;
            });

            // Read the model and create bean definitions based on its content
            ConfigurationClassBeanDefinitionReader reader = new ConfigurationClassBeanDefinitionReader(
                    registry, sourceExtractor, resourceLoader, environment,
                    importBeanNameGenerator, parser.getImportRegistry());
            reader.loadBeanDefinitions(configClasses);
            alreadyParsed.addAll(configClasses);
            processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();

            candidates.clear();
            if (registry.getBeanDefinitionCount() > candidateNames.length) {
                String[] newCandidateNames = registry.getBeanDefinitionNames();
                Set<String> oldCandidateNames = Set.of(candidateNames);
                Set<String> alreadyParsedClasses = new HashSet<>();
                for (ConfigurationClass configurationClass : alreadyParsed) {
                    alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
                }
                for (String candidateName : newCandidateNames) {
                    if (!oldCandidateNames.contains(candidateName)) {
                        BeanDefinition bd = registry.getBeanDefinition(candidateName);
                        if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, metadataReaderFactory) &&
                                !alreadyParsedClasses.contains(bd.getBeanClassName())) {
                            candidates.add(new BeanDefinitionHolder(bd, candidateName));
                        }
                    }
                }
                candidateNames = newCandidateNames;
            }
        }
        while (!candidates.isEmpty());

        // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
//        if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
//            sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
//        }
    }
}
