package com.supos.common.dto.mock;

import cn.hutool.core.util.RandomUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class MockOrderDTO {
    private long id;
    private String customer;
    private String submitTime;
    private int skucount;
    private int count;
    private String deliveryTime;
    private BigDecimal orderPrice;
    private int userID;
    private String userName;
    private int process;
    private int producedCount;
    private int shippedCount;
    private int orderType = 1;
    private int userMaterialStatus;
    private int status = 1;
    private BigDecimal receivableAmount;
    private BigDecimal collectedAmount;
    private BigDecimal overdueAmount;

    private static final String[] CUSTOMERS = {
        "河北雄安素水互联网科技有限公司",
        "上海万汇云建筑科技服务有限公司",
        "兰州美尔康生物科技有限公司",
        "甘肃知行合创生物科技有限公司",
        "兰州宁远化工有限责任公司",
        "乌鲁木齐智星宏元科技有限公司",
        "甘肃润康药业有限公司"
    };

    private static final String[] CUSTOMERS_EN = {
            "Google",
            "Microsoft",
            "Apple",
            "Amazon",
            "Tesla"
    };

    private static final String[] USER_NAMES = {"张学文", "李秋菊"};

    private static final String[] USER_NAMES_EN = {"Elon Musk", "Bill Gates","Mark Zuckerberg"};

    public MockOrderDTO() {
        this.id = RandomUtil.randomInt(0,1000000);
        this.customer = getRandomCustomer();
        this.submitTime = getRandomDateTime();
        this.skucount = getRandomInt(10, 30);
        this.count = getRandomInt(10, 500);
        this.deliveryTime = getRandomDateTime();
        this.orderPrice = BigDecimal.valueOf(getRandomInt(10000, 1000000));
        this.userID = getRandomInt(1, 10);
        this.userName = getRandomUserName();
        this.process = getRandomInt(0, 100);
        this.producedCount = getRandomInt(10, 300);
        this.shippedCount = getRandomInt(10, 200);
        this.userMaterialStatus = getRandomInt(0, 1);
        this.receivableAmount = this.orderPrice;
        this.collectedAmount = BigDecimal.valueOf(getRandomInt(0, 10000));
        this.overdueAmount = BigDecimal.valueOf(getRandomInt(0, 10000));
    }

    private String getRandomCustomer() {
        if ("en-US".equals(System.getenv("SYS_OS_LANG"))){
            return CUSTOMERS_EN[getRandomInt(0, CUSTOMERS_EN.length - 1)];
        } else {
            return CUSTOMERS[getRandomInt(0, CUSTOMERS.length - 1)];
        }
    }

    private String getRandomUserName() {
        if ("en-US".equals(System.getenv("SYS_OS_LANG"))){
            return USER_NAMES_EN[getRandomInt(0, USER_NAMES_EN.length - 1)];
        } else {
            return USER_NAMES[getRandomInt(0, USER_NAMES.length - 1)];
        }
    }

    private String getRandomDateTime() {
        long startEpoch = LocalDateTime.of(2024, 1, 1, 0, 0).atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        long endEpoch = LocalDateTime.of(2025, 1, 31, 23, 59).atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        long randomEpoch = getRandomLong(startEpoch, endEpoch);
        LocalDateTime randomDateTime = LocalDateTime.ofEpochSecond(randomEpoch, 0, java.time.ZoneOffset.UTC);
        return randomDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

    }

    private int getRandomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private long getRandomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

}
