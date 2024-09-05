package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.ArrayList;
import java.util.List;

class InvestigatorLogger implements Logger {

    private final Logger raw;

    private final List<Op> ops = new ArrayList<>();

    InvestigatorLogger(Logger raw) {
        this.raw = raw;
    }

    public void submit() {
        for (Op op : ops) {
            switch (op.level) {
                case INFO:
                    if (op.throwable != null) {
                        raw.info(op.fmt, op.throwable);
                    } else {
                        raw.info(op.fmt, op.arguments);
                    }
                    break;
                case WARN:
                    if (op.throwable != null) {
                        raw.warn(op.fmt, op.throwable);
                    } else {
                        raw.warn(op.fmt, op.arguments);
                    }
                    break;
                case ERROR:
                    if (op.throwable != null) {
                        raw.error(op.fmt, op.throwable);
                    } else {
                        raw.error(op.fmt, op.arguments);
                    }
                    break;
            }
        }
        ops.clear();
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return raw.makeLoggingEventBuilder(level);
    }

    @Override
    @CheckReturnValue
    public LoggingEventBuilder atLevel(Level level) {
        return raw.atLevel(level);
    }

    @Override
    public boolean isEnabledForLevel(Level level) {
        return raw.isEnabledForLevel(level);
    }

    @Override
    public boolean isTraceEnabled() {
        return raw.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        raw.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        raw.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        raw.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        raw.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        raw.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return raw.isTraceEnabled(marker);
    }

    @Override
    @CheckReturnValue
    public LoggingEventBuilder atTrace() {
        return raw.atTrace();
    }

    @Override
    public void trace(Marker marker, String msg) {
        raw.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        raw.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        raw.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        raw.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        raw.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return raw.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        raw.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        raw.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        raw.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        raw.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        raw.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return raw.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        raw.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        raw.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        raw.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        raw.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        raw.debug(marker, msg, t);
    }

    @Override
    @CheckReturnValue
    public LoggingEventBuilder atDebug() {
        return raw.atDebug();
    }

    @Override
    public boolean isInfoEnabled() {
        return raw.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (raw.isInfoEnabled()) {
            ops.add(new Op(Level.INFO, msg));
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (raw.isInfoEnabled()) {
            ops.add(new Op(Level.INFO, format, new Object[]{ arg }));
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (raw.isInfoEnabled()) {
            ops.add(new Op(Level.INFO, format, new Object[]{ arg1, arg2 }));
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (raw.isInfoEnabled()) {
            ops.add(new Op(Level.INFO, format, arguments));
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (raw.isInfoEnabled()) {
            ops.add(new Op(Level.INFO, msg, t));
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return raw.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        raw.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        raw.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        raw.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        raw.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        raw.info(marker, msg, t);
    }

    @Override
    @CheckReturnValue
    public LoggingEventBuilder atInfo() {
        return raw.atInfo();
    }

    @Override
    public boolean isWarnEnabled() {
        return raw.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (raw.isWarnEnabled()) {
            ops.add(new Op(Level.WARN, msg));
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (raw.isWarnEnabled()) {
            ops.add(new Op(Level.WARN, format, new Object[] { arg }));
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (raw.isWarnEnabled()) {
            ops.add(new Op(Level.WARN, format, arguments));
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (raw.isWarnEnabled()) {
            ops.add(new Op(Level.WARN, format, new Object[] { arg1, arg2 }));
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (raw.isWarnEnabled()) {
            ops.add(new Op(Level.WARN, msg, t));
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return raw.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        raw.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        raw.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        raw.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        raw.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        raw.warn(marker, msg, t);
    }

    @Override
    @CheckReturnValue
    public LoggingEventBuilder atWarn() {
        return raw.atWarn();
    }

    @Override
    public boolean isErrorEnabled() {
        return raw.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (raw.isErrorEnabled()) {
            ops.add(new Op(Level.ERROR, msg));
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (raw.isErrorEnabled()) {
            ops.add(new Op(Level.ERROR, format, new Object[] { arg }));
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (raw.isErrorEnabled()) {
            ops.add(new Op(Level.ERROR, format, new Object[] { arg1, arg2 }));
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (raw.isErrorEnabled()) {
            ops.add(new Op(Level.ERROR, format, arguments));
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (raw.isErrorEnabled()) {
            ops.add(new Op(Level.ERROR, msg, t));
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return raw.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        raw.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        raw.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        raw.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        raw.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        raw.error(marker, msg, t);
    }

    @Override
    @CheckReturnValue
    public LoggingEventBuilder atError() {
        return raw.atError();
    }

    private static class Op {

        private static final Object[] EMPTY_ARGS = new Object[0];

        final Level level;

        final String fmt;

        final Object[] arguments;

        final Throwable throwable;

        Op(Level level, String fmt) {
            this(level, fmt, EMPTY_ARGS);
        }

        Op(Level level, String fmt, Object[] arguments) {
            this.level = level;
            this.fmt = fmt;
            this.arguments = arguments;
            this.throwable = null;
        }

        Op(Level level, String fmt, Throwable throwable) {
            this.level = level;
            this.fmt = fmt;
            this.arguments = EMPTY_ARGS;
            this.throwable = throwable;
        }
    }
}
