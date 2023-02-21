package kcud.ContraptionNalgebra;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import static kcud.ContraptionNalgebra.Vectors.*;
import static kcud.ContraptionNalgebra.kdMathDefs.kdSqrt;

public class kdQuaternion
{
    private FloatVector m_intrdata;
    public FloatVector intrinsics() {return this.m_intrdata;}

    public kdQuaternion()
    {
        this.zero();
    }

    public kdQuaternion(final float x, final float y, final float z, final float w)
    {
        this.m_intrdata = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{x,y,z,w}, 0);
    }

    public kdQuaternion(final kdQuaternion q)
    {
        this.m_intrdata = q.m_intrdata;
    }

    public kdQuaternion(FloatVector regquat)
    {
        this.m_intrdata = regquat;
    }

    public kdQuaternion identity()
    {
        this.m_intrdata = Vec128f_identity4;
        return this;
    }

    public kdQuaternion zero()
    {
        this.m_intrdata = Vec128f_zero;
        return this;
    }

    public float x() {return this.m_intrdata.lane(0);}
    public kdQuaternion x(final float x)
    {
        this.m_intrdata = this.m_intrdata.withLane(0, x);
        return this;
    }
    public kdQuaternion x(final float x, kdQuaternion dest) {return dest.set(this).x(x);}
    public float y() {return this.m_intrdata.lane(1);}
    public kdQuaternion y(final float y)
    {
        this.m_intrdata = this.m_intrdata.withLane(1, y);
        return this;
    }
    public kdQuaternion y(final float y, kdQuaternion dest) {return dest.set(this).y(y);}
    public float z() {return this.m_intrdata.lane(2);}
    public kdQuaternion z(final float z)
    {
        this.m_intrdata = this.m_intrdata.withLane(2, z);
        return this;
    }
    public kdQuaternion z(final float z, kdQuaternion dest) {return dest.set(this).z(z);}
    public float w() {return this.m_intrdata.lane(3);}
    public kdQuaternion w(final float w)
    {
        this.m_intrdata = this.m_intrdata.withLane(3, w);
        return this;
    }
    public kdQuaternion w(final float w, kdQuaternion dest) {return dest.set(this).w(w);}

    public kdQuaternion set(final kdQuaternion q)
    {
        this.m_intrdata = q.m_intrdata;
        return this;
    }

    public float dot(final kdQuaternion q)
    {
        return this.m_intrdata.mul(q.m_intrdata).reduceLanes(VectorOperators.ADD);
    }

    public float length2()
    {
        return this.dot(this);
    }

    public float length()
    {
        return kdSqrt(this.length2());
    }

    public kdQuaternion normalize()
    {
        FloatVector dot = this.m_intrdata.mul(this.m_intrdata);
        dot = dot.add(dot.rearrange(VecSwizzle128f_2323));
        dot = dot.rearrange(VecSwizzle128f_0001);
        dot = dot.add(dot.rearrange(VecSwizzle128f_0033));
        dot = dot.rearrange(VecSwizzle128f_2222);
        this.m_intrdata = this.m_intrdata.div(dot.sqrt());
        return this;
    }
    public kdQuaternion normalize(kdQuaternion dest) {return dest.set(this).normalize();}

    public kdQuaternion conjugate()
    {
        this.m_intrdata = this.m_intrdata.mul(Vec128f_none_none_none_one);
        return this;
    }
    public kdQuaternion conjuguate(kdQuaternion dest) {return dest.set(this).conjugate();}

    public kdQuaternion rotationZYX(final kdVector3 angles)
    {
        FloatVector half_angles = angles.intrinsics().mul(0.5f);
        FloatVector cos_angles = half_angles.lanewise(VectorOperators.COS);
        FloatVector sin_angles = half_angles.lanewise(VectorOperators.SIN);

        FloatVector x0 = cos_angles.rearrange(VecSwizzle128f_0040, sin_angles);
        FloatVector x1 = cos_angles.rearrange(VecSwizzle128f_1511, sin_angles);
        FloatVector x2 = cos_angles.rearrange(VecSwizzle128f_6222, sin_angles);
        FloatVector y0 = sin_angles.rearrange(VecSwizzle128f_0040, cos_angles);
        FloatVector y1 = sin_angles.rearrange(VecSwizzle128f_1511, cos_angles);
        FloatVector y2 = sin_angles.rearrange(VecSwizzle128f_6222, cos_angles);

        this.m_intrdata = x0.mul(x1).fma(x2, y0.mul(y1).mul(y2).mul(Vec128f_none_one_none_one));
        return this;
    }

    public kdQuaternion rotationXYZ(final kdVector3 angles)
    {
        FloatVector half_angles = angles.intrinsics().mul(0.5f);
        FloatVector cos_angles = half_angles.lanewise(VectorOperators.COS);
        FloatVector sin_angles = half_angles.lanewise(VectorOperators.SIN);

        FloatVector x0 = cos_angles.rearrange(VecSwizzle128f_3373, sin_angles);
        FloatVector x1 = cos_angles.rearrange(VecSwizzle128f_1511, sin_angles);
        FloatVector x2 = cos_angles.rearrange(VecSwizzle128f_4000, sin_angles);
        FloatVector y0 = sin_angles.rearrange(VecSwizzle128f_3373, cos_angles);
        FloatVector y1 = sin_angles.rearrange(VecSwizzle128f_1511, cos_angles);
        FloatVector y2 = sin_angles.rearrange(VecSwizzle128f_4000, cos_angles);

        this.m_intrdata = x0.mul(x1).fma(x2, y0.mul(y1).mul(y2).mul(Vec128f_none_one_none_one));
        return this;
    }

    public kdQuaternion multiply(final kdQuaternion q)
    {
        FloatVector result = q.m_intrdata;
        result = result.rearrange(VecSwizzle128f_3333);
        FloatVector q2x = q.m_intrdata.rearrange(VecSwizzle128f_0000);
        FloatVector q2y = q.m_intrdata.rearrange(VecSwizzle128f_1111);
        FloatVector q2z = q.m_intrdata.rearrange(VecSwizzle128f_2222);
        result = result.mul(this.m_intrdata);
        FloatVector q1s = this.m_intrdata.rearrange(VecSwizzle128f_3210);
        q2x = q2x.mul(q1s);
        q1s = q1s.rearrange(VecSwizzle128f_1032);
        result = q2x.fma(Vec128f_one_none_one_none, result);
        q2y = q2y.mul(q1s);
        q1s = q1s.rearrange(VecSwizzle128f_3210);
        q2y = q2y.mul(Vec128f_one_one_none_none);
        q2z = q2z.mul(q1s);
        this.m_intrdata = result.add(q2z.fma(Vec128f_none_one_one_none, q2y));
        return this;
    }
    public kdQuaternion multiply(final kdQuaternion q, kdQuaternion dest) {return dest.set(this).multiply(q);}

    @Override
    public String toString()
    {
        return String.format("kdQuaternion[x=%f;y=%f;z=%f;w=%f]", this.x(), this.y(), this.z(), this.w());
    }
}
