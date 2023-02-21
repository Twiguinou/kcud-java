package kcud.ContraptionNalgebra;

import jdk.incubator.vector.FloatVector;

import java.nio.FloatBuffer;

import static kcud.ContraptionNalgebra.Vectors.*;

public class kdMatrix4
{
    private FloatVector m_l0, m_l1, m_l2, m_l3;
    public FloatVector intrinsics1() {return this.m_l0;}
    public FloatVector intrinsics2() {return this.m_l1;}
    public FloatVector intrinsics3() {return this.m_l2;}
    public FloatVector intrinsics4() {return this.m_l3;}

    public kdMatrix4()
    {
        this.zero();
    }

    public kdMatrix4(final kdMatrix4 m)
    {
        this.m_l0 = m.m_l0;
        this.m_l1 = m.m_l1;
        this.m_l2 = m.m_l2;
        this.m_l3 = m.m_l3;
    }

    public kdMatrix4(final float _00, final float _01, final float _02, final float _03,
                     final float _10, final float _11, final float _12, final float _13,
                     final float _20, final float _21, final float _22, final float _23,
                     final float _30, final float _31, final float _32, final float _33)
    {
        this.m_l0 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_00,_01,_02,_03}, 0);
        this.m_l1 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_10,_11,_12,_13}, 0);
        this.m_l2 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_20,_21,_22,_23}, 0);
        this.m_l3 = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{_30,_31,_32,_33}, 0);
    }

    public kdMatrix4 identity()
    {
        this.m_l0 = Vec128f_identity1;
        this.m_l1 = Vec128f_identity2;
        this.m_l2 = Vec128f_identity3;
        this.m_l3 = Vec128f_identity4;
        return this;
    }

    public kdMatrix4 zero()
    {
        this.m_l0 = Vec128f_zero;
        this.m_l1 = Vec128f_zero;
        this.m_l2 = Vec128f_zero;
        this.m_l3 = Vec128f_zero;
        return this;
    }

    public float c00() {return this.m_l0.lane(0);}
    public kdMatrix4 c00(final float c00) {this.m_l0 = this.m_l0.withLane(0, c00);return this;}
    public kdMatrix4 c00(final float c00, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0.withLane(0, c00);
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c01() {return this.m_l0.lane(1);}
    public kdMatrix4 c01(final float c01) {this.m_l0 = this.m_l0.withLane(1, c01);return this;}
    public kdMatrix4 c01(final float c01, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0.withLane(1, c01);
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c02() {return this.m_l0.lane(2);}
    public kdMatrix4 c02(final float c02) {this.m_l0 = this.m_l0.withLane(2, c02);return this;}
    public kdMatrix4 c02(final float c02, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0.withLane(2, c02);
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c03() {return this.m_l0.lane(3);}
    public kdMatrix4 c03(final float c03) {this.m_l0 = this.m_l0.withLane(3, c03);return this;}
    public kdMatrix4 c03(final float c03, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0.withLane(3, c03);
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c10() {return this.m_l1.lane(0);}
    public kdMatrix4 c10(final float c10) {this.m_l1 = this.m_l1.withLane(0, c10);return this;}
    public kdMatrix4 c10(final float c10, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1.withLane(0, c10);
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c11() {return this.m_l1.lane(1);}
    public kdMatrix4 c11(final float c11) {this.m_l1 = this.m_l1.withLane(1, c11);return this;}
    public kdMatrix4 c11(final float c11, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1.withLane(1, c11);
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c12() {return this.m_l1.lane(2);}
    public kdMatrix4 c12(final float c12) {this.m_l1 = this.m_l1.withLane(2, c12);return this;}
    public kdMatrix4 c12(final float c12, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1.withLane(2, c12);
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c13() {return this.m_l1.lane(3);}
    public kdMatrix4 c13(final float c13) {this.m_l1 = this.m_l1.withLane(3, c13);return this;}
    public kdMatrix4 c13(final float c13, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1.withLane(3, c13);
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c20() {return this.m_l2.lane(0);}
    public kdMatrix4 c20(final float c20) {this.m_l2 = this.m_l2.withLane(0, c20);return this;}
    public kdMatrix4 c20(final float c20, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2.withLane(0, c20);
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c21() {return this.m_l2.lane(1);}
    public kdMatrix4 c21(final float c21) {this.m_l2 = this.m_l2.withLane(1, c21);return this;}
    public kdMatrix4 c21(final float c21, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2.withLane(1, c21);
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c22() {return this.m_l2.lane(2);}
    public kdMatrix4 c22(final float c22) {this.m_l2 = this.m_l2.withLane(2, c22);return this;}
    public kdMatrix4 c22(final float c22, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2.withLane(2, c22);
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c23() {return this.m_l2.lane(3);}
    public kdMatrix4 c23(final float c23) {this.m_l2 = this.m_l2.withLane(3, c23);return this;}
    public kdMatrix4 c23(final float c23, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2.withLane(3, c23);
        dest.m_l3 = this.m_l3;
        return dest;
    }
    public float c30() {return this.m_l3.lane(0);}
    public kdMatrix4 c30(final float c30) {this.m_l3 = this.m_l3.withLane(0, c30);return this;}
    public kdMatrix4 c30(final float c30, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3.withLane(0, c30);
        return dest;
    }
    public float c31() {return this.m_l3.lane(1);}
    public kdMatrix4 c31(final float c31) {this.m_l3 = this.m_l3.withLane(1, c31);return this;}
    public kdMatrix4 c31(final float c31, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3.withLane(1, c31);
        return dest;
    }
    public float c32() {return this.m_l3.lane(2);}
    public kdMatrix4 c32(final float c32) {this.m_l3 = this.m_l3.withLane(2, c32);return this;}
    public kdMatrix4 c32(final float c32, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3.withLane(2, c32);
        return dest;
    }
    public float c33() {return this.m_l3.lane(3);}
    public kdMatrix4 c33(final float c33) {this.m_l3 = this.m_l3.withLane(3, c33);return this;}
    public kdMatrix4 c33(final float c33, kdMatrix4 dest)
    {
        dest.m_l0 = this.m_l0;
        dest.m_l1 = this.m_l1;
        dest.m_l2 = this.m_l2;
        dest.m_l3 = this.m_l3.withLane(3, c33);
        return dest;
    }

    public kdMatrix4 set(final kdMatrix4 m)
    {
        this.m_l0 = m.m_l0;
        this.m_l1 = m.m_l1;
        this.m_l2 = m.m_l2;
        this.m_l3 = m.m_l3;
        return this;
    }

    public kdMatrix4 multiply(final kdMatrix4 m)
    {
        final FloatVector r0, r1, r2, r3;
        FloatVector ns0, ns1, ns2;

        ns0 = this.m_l0.mul(m.m_l0.rearrange(VecSwizzle128f_0000));
        ns1 = this.m_l1.fma(m.m_l0.rearrange(VecSwizzle128f_1111), ns0);
        ns2 = this.m_l2.fma(m.m_l0.rearrange(VecSwizzle128f_2222), ns1);
        r0 = this.m_l3.fma(m.m_l0.rearrange(VecSwizzle128f_3333), ns2);

        ns0 = this.m_l0.mul(m.m_l1.rearrange(VecSwizzle128f_0000));
        ns1 = this.m_l1.fma(m.m_l1.rearrange(VecSwizzle128f_1111), ns0);
        ns2 = this.m_l2.fma(m.m_l1.rearrange(VecSwizzle128f_2222), ns1);
        r1 = this.m_l3.fma(m.m_l1.rearrange(VecSwizzle128f_3333), ns2);

        ns0 = this.m_l0.mul(m.m_l2.rearrange(VecSwizzle128f_0000));
        ns1 = this.m_l1.fma(m.m_l2.rearrange(VecSwizzle128f_1111), ns0);
        ns2 = this.m_l2.fma(m.m_l2.rearrange(VecSwizzle128f_2222), ns1);
        r2 = this.m_l3.fma(m.m_l2.rearrange(VecSwizzle128f_3333), ns2);

        ns0 = this.m_l0.mul(m.m_l3.rearrange(VecSwizzle128f_0000));
        ns1 = this.m_l1.fma(m.m_l3.rearrange(VecSwizzle128f_1111), ns0);
        ns2 = this.m_l2.fma(m.m_l3.rearrange(VecSwizzle128f_2222), ns1);
        r3 = this.m_l3.fma(m.m_l3.rearrange(VecSwizzle128f_3333), ns2);

        this.m_l0 = r0;
        this.m_l1 = r1;
        this.m_l2 = r2;
        this.m_l3 = r3;
        return this;
    }
    public kdMatrix4 multiply(final kdMatrix4 m, kdMatrix4 dest) {return dest.set(this).multiply(m);}
    
    public void get(FloatBuffer buffer)
    {
        if (buffer.remaining() < 16) return;
        buffer.put(buffer.position(), this.c00());
        buffer.put(buffer.position() + 1, this.c01());
        buffer.put(buffer.position() + 2, this.c02());
        buffer.put(buffer.position() + 3, this.c03());
        buffer.put(buffer.position() + 4, this.c10());
        buffer.put(buffer.position() + 5, this.c11());
        buffer.put(buffer.position() + 6, this.c12());
        buffer.put(buffer.position() + 7, this.c13());
        buffer.put(buffer.position() + 8, this.c20());
        buffer.put(buffer.position() + 9, this.c21());
        buffer.put(buffer.position() + 10, this.c22());
        buffer.put(buffer.position() + 11, this.c23());
        buffer.put(buffer.position() + 12, this.c30());
        buffer.put(buffer.position() + 13, this.c31());
        buffer.put(buffer.position() + 14, this.c32());
        buffer.put(buffer.position() + 15, this.c33());
    }

    public kdMatrix4 set(final FloatBuffer buffer)
    {
        if (buffer.remaining() < 16) return this;
        this.c00(buffer.get(buffer.position()));
        this.c01(buffer.get(buffer.position()) + 1);
        this.c02(buffer.get(buffer.position()) + 2);
        this.c03(buffer.get(buffer.position()) + 3);
        this.c10(buffer.get(buffer.position()) + 4);
        this.c11(buffer.get(buffer.position()) + 5);
        this.c12(buffer.get(buffer.position()) + 6);
        this.c13(buffer.get(buffer.position()) + 7);
        this.c20(buffer.get(buffer.position()) + 8);
        this.c21(buffer.get(buffer.position()) + 9);
        this.c22(buffer.get(buffer.position()) + 10);
        this.c23(buffer.get(buffer.position()) + 11);
        this.c30(buffer.get(buffer.position()) + 12);
        this.c31(buffer.get(buffer.position()) + 13);
        this.c32(buffer.get(buffer.position()) + 14);
        this.c33(buffer.get(buffer.position()) + 15);
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
}
