package kcud.ContraptionNalgebra;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorShuffle;

public final class Vectors
{private Vectors() {}

    public static final VectorShuffle<Float> VecSwizzle128f_0000 = FloatVector.SPECIES_128.shuffleFromValues(0, 0, 0, 0);
    public static final VectorShuffle<Float> VecSwizzle128f_1111 = FloatVector.SPECIES_128.shuffleFromValues(1, 1, 1, 1);
    public static final VectorShuffle<Float> VecSwizzle128f_2222 = FloatVector.SPECIES_128.shuffleFromValues(2, 2, 2, 2);
    public static final VectorShuffle<Float> VecSwizzle128f_3333 = FloatVector.SPECIES_128.shuffleFromValues(3, 3, 3, 3);
    public static final VectorShuffle<Float> VecSwizzle128f_1212 = FloatVector.SPECIES_128.shuffleFromValues(1, 2, 1, 2);
    public static final VectorShuffle<Float> VecSwizzle128f_1203 = FloatVector.SPECIES_128.shuffleFromValues(1, 2, 0, 3);
    public static final VectorShuffle<Float> VecSwizzle128f_2323 = FloatVector.SPECIES_128.shuffleFromValues(2, 3, 2, 3);
    public static final VectorShuffle<Float> VecSwizzle128f_0001 = FloatVector.SPECIES_128.shuffleFromValues(0, 0, 0, 1);
    public static final VectorShuffle<Float> VecSwizzle128f_0033 = FloatVector.SPECIES_128.shuffleFromValues(0, 0, 3, 3);
    public static final VectorShuffle<Float> VecSwizzle128f_3210 = FloatVector.SPECIES_128.shuffleFromValues(3, 2, 1, 0);
    public static final VectorShuffle<Float> VecSwizzle128f_1032 = FloatVector.SPECIES_128.shuffleFromValues(1, 0, 3, 2);

    public static final VectorShuffle<Float> VecSwizzle128f_0040 = FloatVector.SPECIES_128.shuffleFromValues(0, 0, 4, 2);
    public static final VectorShuffle<Float> VecSwizzle128f_1511 = FloatVector.SPECIES_128.shuffleFromValues(1, 5, 1, 1);
    public static final VectorShuffle<Float> VecSwizzle128f_6222 = FloatVector.SPECIES_128.shuffleFromValues(6, 2, 2, 2);
    public static final VectorShuffle<Float> VecSwizzle128f_4000 = FloatVector.SPECIES_128.shuffleFromValues(3, 3, 7, 3);
    public static final VectorShuffle<Float> VecSwizzle128f_3373 = FloatVector.SPECIES_128.shuffleFromValues(4, 0, 0, 0);

    public static final FloatVector Vec128f_zero = FloatVector.zero(FloatVector.SPECIES_128);
    public static final FloatVector Vec128f_nzero = FloatVector.broadcast(FloatVector.SPECIES_128, -0.f);
    public static final FloatVector Vec128f_one = FloatVector.broadcast(FloatVector.SPECIES_128, 1.f);
    public static final FloatVector Vec128f_identity1 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{1.f, 0.f, 0.f, 0.f}, 0);
    public static final FloatVector Vec128f_identity2 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{0.f, 1.f, 0.f, 0.f}, 0);
    public static final FloatVector Vec128f_identity3 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{0.f, 0.f, 1.f, 0.f}, 0);
    public static final FloatVector Vec128f_identity4 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{0.f, 0.f, 0.f, 1.f}, 0);
    public static final FloatVector Vec128f_none_none_none_one = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{-1.f, -1.f, -1.f, 1.f}, 0);
    public static final FloatVector Vec128f_one_none_one_none = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{1.f, -1.f, 1.f, -1.f}, 0);
    public static final FloatVector Vec128f_one_one_none_none = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{1.f, 1.f, -1.f, -1.f}, 0);
    public static final FloatVector Vec128f_none_one_one_none = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{-1.f, 1.f, 1.f, -1.f}, 0);
    public static final FloatVector Vec128f_none_one_none_one = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{-1.f, 1.f, -1.f, 1.f}, 0);
}
