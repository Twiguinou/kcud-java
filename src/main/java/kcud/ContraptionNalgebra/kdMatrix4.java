package kcud.ContraptionNalgebra;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorShuffle;

public class kdMatrix4
{

    private FloatVector m_l0, m_l1, m_l2, m_l3;

    public kdMatrix4()
    {
        this.m_l0 = __kdMatrix4_zero128;
        this.m_l1 = __kdMatrix4_zero128;
        this.m_l2 = __kdMatrix4_zero128;
        this.m_l3 = __kdMatrix4_zero128;
    }

    public kdMatrix4(final kdMatrix4 m)
    {
        this.m_l0 = m.m_l0;
        this.m_l1 = m.m_l1;
        this.m_l2 = m.m_l2;
        this.m_l3 = m.m_l3;
    }

    public kdMatrix4(float _00, float _01, float _02, float _03,
                     float _10, float _11, float _12, float _13,
                     float _20, float _21, float _22, float _23,
                     float _30, float _31, float _32, float _33)
    {
        this.m_l0 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_00,_01,_02,_03}, 0);
        this.m_l1 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_10,_11,_12,_13}, 0);
        this.m_l2 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_20,_21,_22,_23}, 0);
        this.m_l3 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_30,_31,_32,_33}, 0);
    }

    public float c00() {return this.m_l0.lane(0);}
    public void c00(float c00) {this.m_l0 = this.m_l0.withLane(0, c00);}
    public float c01() {return this.m_l0.lane(1);}
    public void c01(float c01) {this.m_l0 = this.m_l0.withLane(1, c01);}
    public float c02() {return this.m_l0.lane(2);}
    public void c02(float c02) {this.m_l0 = this.m_l0.withLane(2, c02);}
    public float c03() {return this.m_l0.lane(3);}
    public void c03(float c03) {this.m_l0 = this.m_l0.withLane(3, c03);}
    public float c10() {return this.m_l1.lane(0);}
    public void c10(float c10) {this.m_l1 = this.m_l1.withLane(0, c10);}
    public float c11() {return this.m_l1.lane(1);}
    public void c11(float c11) {this.m_l1 = this.m_l1.withLane(1, c11);}
    public float c12() {return this.m_l1.lane(2);}
    public void c12(float c12) {this.m_l1 = this.m_l1.withLane(2, c12);}
    public float c13() {return this.m_l1.lane(3);}
    public void c13(float c13) {this.m_l1 = this.m_l1.withLane(3, c13);}
    public float c20() {return this.m_l2.lane(0);}
    public void c20(float c20) {this.m_l2 = this.m_l2.withLane(0, c20);}
    public float c21() {return this.m_l2.lane(1);}
    public void c21(float c21) {this.m_l2 = this.m_l2.withLane(1, c21);}
    public float c22() {return this.m_l2.lane(2);}
    public void c22(float c22) {this.m_l2 = this.m_l2.withLane(2, c22);}
    public float c23() {return this.m_l2.lane(3);}
    public void c23(float c23) {this.m_l2 = this.m_l2.withLane(3, c23);}
    public float c30() {return this.m_l3.lane(0);}
    public void c30(float c30) {this.m_l3 = this.m_l3.withLane(0, c30);}
    public float c31() {return this.m_l3.lane(1);}
    public void c31(float c31) {this.m_l3 = this.m_l3.withLane(1, c31);}
    public float c32() {return this.m_l3.lane(2);}
    public void c32(float c32) {this.m_l3 = this.m_l3.withLane(2, c32);}
    public float c33() {return this.m_l3.lane(3);}
    public void c33(float c33) {this.m_l3 = this.m_l3.withLane(3, c33);}

    public kdMatrix4 multiply(final kdMatrix4 m)
    {
        FloatVector nsl0 = this.m_l0.rearrange(__kdMatrix4_shuffle0000);
        FloatVector nsl1 = this.m_l0.rearrange(__kdMatrix4_shuffle1111);
        FloatVector nsl2 = this.m_l0.rearrange(__kdMatrix4_shuffle2222);
        FloatVector nsl3 = this.m_l0.rearrange(__kdMatrix4_shuffle3333);
        nsl0 = nsl0.mul(m.m_l0);
        nsl1 = nsl1.mul(m.m_l1);
        nsl2 = nsl2.mul(m.m_l2);
        nsl3 = nsl3.mul(m.m_l3);
        nsl0 = nsl0.add(nsl2);
        nsl1 = nsl1.add(nsl3);
        nsl0 = nsl0.add(nsl1);
        this.m_l0 = nsl0;
        nsl0 = this.m_l1.rearrange(__kdMatrix4_shuffle0000);
        nsl1 = this.m_l1.rearrange(__kdMatrix4_shuffle1111);
        nsl2 = this.m_l1.rearrange(__kdMatrix4_shuffle2222);
        nsl3 = this.m_l1.rearrange(__kdMatrix4_shuffle3333);
        nsl0 = nsl0.mul(m.m_l0);
        nsl1 = nsl1.mul(m.m_l1);
        nsl2 = nsl2.mul(m.m_l2);
        nsl3 = nsl3.mul(m.m_l3);
        nsl0 = nsl0.add(nsl2);
        nsl1 = nsl1.add(nsl3);
        nsl0 = nsl0.add(nsl1);
        this.m_l1 = nsl0;
        nsl0 = this.m_l2.rearrange(__kdMatrix4_shuffle0000);
        nsl1 = this.m_l2.rearrange(__kdMatrix4_shuffle1111);
        nsl2 = this.m_l2.rearrange(__kdMatrix4_shuffle2222);
        nsl3 = this.m_l2.rearrange(__kdMatrix4_shuffle3333);
        nsl0 = nsl0.mul(m.m_l0);
        nsl1 = nsl1.mul(m.m_l1);
        nsl2 = nsl2.mul(m.m_l2);
        nsl3 = nsl3.mul(m.m_l3);
        nsl0 = nsl0.add(nsl2);
        nsl1 = nsl1.add(nsl3);
        nsl0 = nsl0.add(nsl1);
        this.m_l2 = nsl0;
        nsl0 = this.m_l3.rearrange(__kdMatrix4_shuffle0000);
        nsl1 = this.m_l3.rearrange(__kdMatrix4_shuffle1111);
        nsl2 = this.m_l3.rearrange(__kdMatrix4_shuffle2222);
        nsl3 = this.m_l3.rearrange(__kdMatrix4_shuffle3333);
        nsl0 = nsl0.mul(m.m_l0);
        nsl1 = nsl1.mul(m.m_l1);
        nsl2 = nsl2.mul(m.m_l2);
        nsl3 = nsl3.mul(m.m_l3);
        nsl0 = nsl0.add(nsl2);
        nsl1 = nsl1.add(nsl3);
        nsl0 = nsl0.add(nsl1);
        this.m_l3 = nsl0;
        return this;
    }

    public kdMatrix4 transpose()
    {
        FloatVector t0 = this.m_l0.rearrange(__kdMatrix4_shuffle0145, this.m_l1);
        FloatVector t1 = this.m_l2.rearrange(__kdMatrix4_shuffle0145, this.m_l3);
        FloatVector t2 = this.m_l0.rearrange(__kdMatrix4_shuffle2367, this.m_l1);
        FloatVector t3 = this.m_l2.rearrange(__kdMatrix4_shuffle2367, this.m_l3);
        this.m_l0 = t0.rearrange(__kdMatrix4_shuffle0246, t1);
        this.m_l1 = t0.rearrange(__kdMatrix4_shuffle1357, t1);
        this.m_l2 = t2.rearrange(__kdMatrix4_shuffle0246, t3);
        this.m_l3 = t2.rearrange(__kdMatrix4_shuffle1357, t3);
        return this;
    }

    @Override
    public String toString()
    {
        return String.format("kdMatrix4[\n%f;%f;%f;%f\n%f;%f;%f;%f\n%f;%f;%f;%f\n%f;%f;%f;%f\n]",
                this.c00(), this.c01(), this.c02(), this.c03(),
                this.c10(), this.c11(), this.c12(), this.c13(),
                this.c20(), this.c21(), this.c22(), this.c23(),
                this.c30(), this.c31(), this.c32(), this.c33());
    }

    private static final VectorShuffle<Float> __kdMatrix4_shuffle0000 = FloatVector.SPECIES_128.shuffleFromValues(0, 0, 0, 0);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle1111 = FloatVector.SPECIES_128.shuffleFromValues(1, 1, 1, 1);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle2222 = FloatVector.SPECIES_128.shuffleFromValues(2, 2, 2, 2);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle3333 = FloatVector.SPECIES_128.shuffleFromValues(3, 3, 3, 3);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle0145 = FloatVector.SPECIES_128.shuffleFromValues(0, 1, 4, 5);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle2367 = FloatVector.SPECIES_128.shuffleFromValues(2, 3, 6, 7);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle0246 = FloatVector.SPECIES_128.shuffleFromValues(0, 2, 4, 6);
    private static final VectorShuffle<Float> __kdMatrix4_shuffle1357 = FloatVector.SPECIES_128.shuffleFromValues(1, 3, 5, 7);
    private static final FloatVector __kdMatrix4_zero128 = FloatVector.zero(FloatVector.SPECIES_128);
}
