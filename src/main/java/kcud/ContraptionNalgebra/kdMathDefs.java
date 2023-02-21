package kcud.ContraptionNalgebra;

public final class kdMathDefs
{private kdMathDefs() {}

    public static final float KCUD_PI = 3.1415927f;
    public static final float KCUD_PI_2 = 1.5707964f;
    public static final float KCUD_PI_4 = .78539816f;
    public static final float KCUD_2PI = 6.2831855f;

    private static final float KCUD_RADIANS_CNV = 0.017453293f;
    private static final float KCUD_DEGREES_CNV = 57.29577951f;
    public static float kdRadians(final float x)
    {
        return x * KCUD_RADIANS_CNV;
    }
    public static float kdDegrees(final float x)
    {
        return x * KCUD_DEGREES_CNV;
    }

    public static float kdSqrt(final float x) {return (float)Math.sqrt(x);}

    public static float kdTan(final float x)
    {
        return (float)Math.tan(x);
    }

    public static float kdSin(final float x)
    {
        return (float)Math.sin(x);
    }

    public static float kdCos(final float x)
    {
        return (float)Math.cos(x);
    }

    public static float kdAtan2(final float y, final float x)
    {
        return (float)Math.atan2(y, x);
    }

    public static float kdAcos(final float x)
    {
        return (float)Math.acos(x);
    }

    public static float kdPow(final float x, final float exponent)
    {
        return (float)Math.pow(x, exponent);
    }

    public static float kdClamp(final float x, final float a, final float b)
    {
        return Math.max(Math.min(x, b), a);
    }
}
