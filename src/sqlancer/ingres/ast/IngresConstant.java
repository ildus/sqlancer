package sqlancer.ingres.ast;

import sqlancer.IgnoreMeException;
import sqlancer.ingres.IngresSchema.IngresDataType;

public abstract class IngresConstant implements IngresExpression {

    public abstract String getTextRepresentation();

    public abstract String getUnquotedTextRepresentation();

    public static class BooleanConstant extends IngresConstant {

        private final boolean value;

        public BooleanConstant(boolean value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return value ? "TRUE" : "FALSE";
        }

        @Override
        public IngresDataType getExpressionType() {
            return IngresDataType.BOOLEAN;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public IngresConstant isEquals(IngresConstant rightVal) {
            if (rightVal.isNull()) {
                return IngresConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return IngresConstant.createBooleanConstant(value == rightVal.asBoolean());
            } else if (rightVal.isString()) {
                return IngresConstant
                        .createBooleanConstant(value == rightVal.cast(IngresDataType.BOOLEAN).asBoolean());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected IngresConstant isLessThan(IngresConstant rightVal) {
            if (rightVal.isNull()) {
                return IngresConstant.createNullConstant();
            } else if (rightVal.isString()) {
                return isLessThan(rightVal.cast(IngresDataType.BOOLEAN));
            } else {
                assert rightVal.isBoolean();
                return IngresConstant.createBooleanConstant((value ? 1 : 0) < (rightVal.asBoolean() ? 1 : 0));
            }
        }

        @Override
        public IngresConstant cast(IngresDataType type) {
            switch (type) {
            case BOOLEAN:
                return this;
            case INT:
                return IngresConstant.createIntConstant(value ? 1 : 0);
            case VARCHAR:
                return IngresConstant.createTextConstant(value ? "true" : "false");
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class IngresNullConstant extends IngresConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public IngresDataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public IngresConstant isEquals(IngresConstant rightVal) {
            return IngresConstant.createNullConstant();
        }

        @Override
        protected IngresConstant isLessThan(IngresConstant rightVal) {
            return IngresConstant.createNullConstant();
        }

        @Override
        public IngresConstant cast(IngresDataType type) {
            return IngresConstant.createNullConstant();
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class StringConstant extends IngresConstant {

        private final String value;

        public StringConstant(String value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("'%s'", value.replace("'", "''"));
        }

        @Override
        public IngresConstant isEquals(IngresConstant rightVal) {
            if (rightVal.isNull()) {
                return IngresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(IngresDataType.INT).isEquals(rightVal.cast(IngresDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(IngresDataType.BOOLEAN).isEquals(rightVal.cast(IngresDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return IngresConstant.createBooleanConstant(value.contentEquals(rightVal.asString()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected IngresConstant isLessThan(IngresConstant rightVal) {
            if (rightVal.isNull()) {
                return IngresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(IngresDataType.INT).isLessThan(rightVal.cast(IngresDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(IngresDataType.BOOLEAN).isLessThan(rightVal.cast(IngresDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return IngresConstant.createBooleanConstant(value.compareTo(rightVal.asString()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public IngresConstant cast(IngresDataType type) {
            if (type == IngresDataType.VARCHAR) {
                return this;
            }
            String s = value.trim();
            switch (type) {
            case BOOLEAN:
                try {
                    return IngresConstant.createBooleanConstant(Long.parseLong(s) != 0);
                } catch (NumberFormatException e) {
                }
                switch (s.toUpperCase()) {
                case "TRUE":
                    return IngresConstant.createTrue();
                case "FALSE":
                default:
                    return IngresConstant.createFalse();
                }
            case INT:
                try {
                    return IngresConstant.createIntConstant(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    return IngresConstant.createIntConstant(-1);
                }
            case VARCHAR:
                return this;
            default:
                return null;
            }
        }

        @Override
        public IngresDataType getExpressionType() {
            return IngresDataType.VARCHAR;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return value;
        }

    }

    public static class IntConstant extends IngresConstant {

        private final long val;

        public IntConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public IngresDataType getExpressionType() {
            return IngresDataType.INT;
        }

        @Override
        public long asInt() {
            return val;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public IngresConstant isEquals(IngresConstant rightVal) {
            if (rightVal.isNull()) {
                return IngresConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return cast(IngresDataType.BOOLEAN).isEquals(rightVal);
            } else if (rightVal.isInt()) {
                return IngresConstant.createBooleanConstant(val == rightVal.asInt());
            } else if (rightVal.isString()) {
                return IngresConstant.createBooleanConstant(val == rightVal.cast(IngresDataType.INT).asInt());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected IngresConstant isLessThan(IngresConstant rightVal) {
            if (rightVal.isNull()) {
                return IngresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return IngresConstant.createBooleanConstant(val < rightVal.asInt());
            } else if (rightVal.isBoolean()) {
                throw new AssertionError(rightVal);
            } else if (rightVal.isString()) {
                return IngresConstant.createBooleanConstant(val < rightVal.cast(IngresDataType.INT).asInt());
            } else {
                throw new IgnoreMeException();
            }

        }

        @Override
        public IngresConstant cast(IngresDataType type) {
            switch (type) {
            case BOOLEAN:
                return IngresConstant.createBooleanConstant(val != 0);
            case INT:
                return this;
            case VARCHAR:
                return IngresConstant.createTextConstant(String.valueOf(val));
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static IngresConstant createNullConstant() {
        return new IngresNullConstant();
    }

    public String asString() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isString() {
        return false;
    }

    public static IngresConstant createIntConstant(long val) {
        return new IntConstant(val);
    }

    public static IngresConstant createBooleanConstant(boolean val) {
        return new BooleanConstant(val);
    }

    @Override
    public IngresConstant getExpectedValue() {
        return this;
    }

    public boolean isNull() {
        return false;
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException(this.toString());
    }

    public static IngresConstant createFalse() {
        return createBooleanConstant(false);
    }

    public static IngresConstant createTrue() {
        return createBooleanConstant(true);
    }

    public long asInt() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isBoolean() {
        return false;
    }

    public abstract IngresConstant isEquals(IngresConstant rightVal);

    public boolean isInt() {
        return false;
    }

    protected abstract IngresConstant isLessThan(IngresConstant rightVal);

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    public abstract IngresConstant cast(IngresDataType type);

    public static IngresConstant createTextConstant(String string) {
        return new StringConstant(string);
    }

    public abstract static class IngresConstantBase extends IngresConstant {

        @Override
        public String getUnquotedTextRepresentation() {
            return null;
        }

        @Override
        public IngresConstant isEquals(IngresConstant rightVal) {
            return null;
        }

        @Override
        protected IngresConstant isLessThan(IngresConstant rightVal) {
            return null;
        }

        @Override
        public IngresConstant cast(IngresDataType type) {
            return null;
        }
    }

    public static class FloatConstant extends IngresConstantBase {

        private final float val;

        public FloatConstant(float val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(val)) {
                return String.valueOf(val);
            } else {
                return "'" + val + "'";
            }
        }

        @Override
        public IngresDataType getExpressionType() {
            return IngresDataType.FLOAT;
        }

    }

    public static IngresConstant createFloatConstant(float val) {
        return new FloatConstant(val);
    }

}
