package org.dromara.common.mybatis.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 轻量级高性能 SQL 日志打印拦截器，替代 p6spy
 * 打印完整可执行 SQL、执行耗时及 MappedStatement 来源
 *
 * @author Lion Li / Fawkes
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlLogInterceptor implements Interceptor {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            printSqlLog(invocation, cost);
        }
    }

    /**
     * 提取并打印 SQL 日志
     *
     * @param invocation 调用对象
     * @param cost       耗时毫秒
     */
    private void printSqlLog(Invocation invocation, long cost) {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args.length > 1 ? args[1] : null;
            BoundSql boundSql = args.length == 6 ? (BoundSql) args[5] : ms.getBoundSql(parameter);
            Configuration configuration = ms.getConfiguration();
            String sql = formatSql(configuration, boundSql);
            log.info("\n==>  Executing: {}\n==>     Mapper: {} [Cost: {} ms]", sql, ms.getId(), cost);
        } catch (Exception e) {
            log.debug("SqlLogInterceptor format error", e);
        }
    }

    /**
     * 拼接参数并格式化 SQL
     *
     * @param configuration 配置对象
     * @param boundSql      BoundSql
     * @return 完整 SQL
     */
    private String formatSql(Configuration configuration, BoundSql boundSql) {
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ").trim();
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty() || parameterObject == null) {
            return sql;
        }
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            return sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
        }
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        for (ParameterMapping parameterMapping : parameterMappings) {
            String propertyName = parameterMapping.getProperty();
            Object value = null;
            if (metaObject.hasGetter(propertyName)) {
                value = metaObject.getValue(propertyName);
            } else if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            }
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(value)));
        }
        return sql;
    }

    /**
     * 格式化参数取值
     *
     * @param obj 参数对象
     * @return 字符串值
     */
    private String getParameterValue(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "'" + obj + "'";
        }
        if (obj instanceof Date) {
            return "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) obj) + "'";
        }
        if (obj instanceof TemporalAccessor) {
            return "'" + DATE_TIME_FORMATTER.format((TemporalAccessor) obj) + "'";
        }
        return obj.toString();
    }
}
