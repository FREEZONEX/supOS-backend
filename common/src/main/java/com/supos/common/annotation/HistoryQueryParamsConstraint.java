package com.supos.common.annotation;

import com.supos.common.Constants;
import com.supos.common.adpater.historyquery.*;
import com.supos.common.utils.DateTimeUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class HistoryQueryParamsConstraint implements ConstraintValidator<HistoryQueryParamsValidator, HistoryQueryParams> {

    @Override
    public boolean isValid(HistoryQueryParams params, ConstraintValidatorContext context) {
        if (params == null) {
            return false;
        }
        IntervalWindow intervalWindow = params.getIntervalWindow();
        if (intervalWindow != null) {
            WindowTimeConstraint constraint = new WindowTimeConstraint();
            constraint.initialize(IntervalWindow.class.getAnnotation(WindowTimeValidator.class));
            if (!constraint.isValid(intervalWindow, context)) {
                return false;
            }
        }
        int limit = params.getLimit();
        if (limit < 1) {
            setErrMsg(context, "limit", "uns.hist.limit.min");
            return false;
        } else if (limit > 10000) {
            setErrMsg(context, "limit", "uns.hist.limit.max");
            return false;
        }
        if (params.getOffset() < 0) {
            setErrMsg(context, "offset", "uns.hist.offset.min");
            return false;
        }
        if (ArrayUtils.isEmpty(params.getSelect())) {
            setErrMsg(context, "select", "uns.hist.select.empty");
            return false;
        }
        Where where = params.getWhere();
        if (where == null || where.isEmpty()) {
            setErrMsg(context, "where", "uns.hist.where.empty");
            return false;
        }
        for (WhereCondition condition : where.getAnd()) {
            if (checkWhereCondition(context, condition)) return false;
        }
        for (WhereCondition condition : where.getOr()) {
            if (checkWhereCondition(context, condition)) return false;
        }

        for (Select select : params.getSelect()) {
            if (StringUtils.isEmpty(select.getTable())) {
                setErrMsg(context, "select.table", "uns.hist.select.table.empty");
                return false;
            }
            if (StringUtils.isEmpty(select.getColumn())) {
                setErrMsg(context, "select.column", "uns.hist.select.column.empty");
                return false;
            }
        }
        if (!replaceWhereTimestamp(params, context)) {
            return false;
        }

        for (Select select : params.getSelect()) {
            if (select.getFunction() != null) {
                if (params.getIntervalWindow() == null) {
                    setErrMsg(context, "groupBy", "uns.hist.groupBy.empty");
                    return false;
                }
                break;
            }
        }
        return true;
    }

    private static boolean checkWhereCondition(ConstraintValidatorContext context, WhereCondition condition) {
        if (StringUtils.isEmpty(condition.getName())) {
            setErrMsg(context, "where.name", "uns.hist.where.name.empty");
            return true;
        }
        if (condition.getOp() == null) {
            setErrMsg(context, "where.op", "uns.hist.where.op.empty");
            return true;
        }
        if (StringUtils.isEmpty(condition.getValue())) {
            setErrMsg(context, "where.value", "uns.hist.where.value.empty");
            return true;
        }
        return false;
    }

    private static boolean replaceWhereTimestamp(HistoryQueryParams params, ConstraintValidatorContext context) {
        long minTime = 0, maxTime = 0;
        Where where = params.getWhere();
        for (WhereCondition condition : where.getAnd()) {
            int isCt = replaceConditionTimestamp(condition);
            if (isCt < 0) {
                setErrMsg(context, "where.value", "uns.invalid.stream.time", condition.getValue());
                return false;
            }
            int directNow = condition.getOp().direction;
            if (isCt == 1 && directNow != 0) {
                Instant tm = condition.getTime();
                if (tm != null) {
                    long mills = tm.toEpochMilli();
                    if (mills > 0) {
                        if (directNow < 0) {
                            maxTime = maxTime == 0 || mills > maxTime ? mills : maxTime;
                        } else {
                            minTime = minTime == 0 || mills < minTime ? mills : minTime;
                        }
                    }
                }
            }
        }
        params.setMinTime(minTime);
        params.setMaxTime(maxTime);
        if (minTime > 0 && maxTime > 0) {
            long durationMills = maxTime - minTime;
            if (durationMills > 0 && params.getLimit() > 1) {

                if (params.getIntervalWindow() == null) {
                    double mills = durationMills * 1.0 / (params.getLimit() - 1);
                    long intervalMills = (long) mills;
                    if (intervalMills >= 1000) {// 时间窗口至少1秒
                        IntervalWindow window = new IntervalWindow();
                        window.setInterval(((long) (mills / 1000)) + "s");
                        window.setIntervalMills(intervalMills);
                        params.setIntervalWindow(window);
                        params.setSamples(params.getLimit());
                    }
                } else {
                    long intervalMills = params.getIntervalWindow().getIntervalMills();
                    long sample = durationMills / intervalMills;
                    if (sample > 0) {
                        params.setSamples((int) Math.min(sample, params.getLimit()));
                    }
                }
            }
        }

        for (WhereCondition condition : where.getOr()) {
            if (replaceConditionTimestamp(condition) < 0) {
                setErrMsg(context, "where.value", "uns.invalid.stream.time", condition.getValue());
                return false;
            }
        }

        return true;
    }


    private static int replaceConditionTimestamp(WhereCondition condition) {
        String name = condition.getName(), value = condition.getValue();
        if (name.equals(Constants.SYS_FIELD_CREATE_TIME) || "_ct".equals(name)) {
            Instant utcDate = DateTimeUtils.parseDate(value);
            if (utcDate != null) {
                condition.setTime(utcDate);
                return 1;
            } else {
                return -1;
            }
        }
        return 0;
    }
}
