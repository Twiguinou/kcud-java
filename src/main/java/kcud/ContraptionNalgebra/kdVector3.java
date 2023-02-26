package kcud.ContraptionNalgebra;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import static kcud.ContraptionNalgebra.Vectors.*;
import static kcud.ContraptionNalgebra.kdMathDefs.*;

public class kdVector3
{
    private FloatVector m_intrdata;
    public FloatVector intrinsics() {return this.m_intrdata;}

    public kdVector3()
    {
        this.zero();
    }

    public kdVector3(final float x, final float y, final float z)
    {
        this.m_intrdata = FloatVector.fromArray(FloatVector.SPECIES_128, new float[]{x,y,z,0.f}, 0);
    }

    public kdVector3(final kdVector3 v)
    {
        this.m_intrdata = v.m_intrdata;
    }

    public kdVector3(FloatVector regvec)
    {
        this.m_intrdata = regvec;
    }

    public kdVector3 zero()
    {
        this.m_intrdata = Vec128f_zero;
        return this;
    }

    public float x() {return this.m_intrdata.lane(0);}
    public kdVector3 x(final float x)
    {
        this.m_intrdata = this.m_intrdata.withLane(0, x);
        return this;
    }
    public kdVector3 x(final float x, kdVector3 dest) {return dest.set(this).x(x);}
    public float y() {return this.m_intrdata.lane(1);}
    public kdVector3 y(final float y)
    {
        this.m_intrdata = this.m_intrdata.withLane(1, y);
        return this;
    }
    public kdVector3 y(final float y, kdVector3 dest) {return dest.set(this).y(y);}
    public float z() {return this.m_intrdata.lane(2);}
    public kdVector3 z(final float z)
    {
        this.m_intrdata = this.m_intrdata.withLane(2, z);
        return this;
    }
    public kdVector3 z(final float z, kdVector3 dest) {return dest.set(this).z(z);}

    public kdVector3 set(final kdVector3 v)
    {
        this.m_intrdata = v.m_intrdata;
        return this;
    }

    public kdVector3 add(final kdVector3 v)
    {
        this.m_intrdata = this.m_intrdata.add(v.m_intrdata);
        return this;
    }
    public kdVector3 add(final kdVector3 v, kdVector3 dest) {return dest.set(this).add(v);}

    public kdVector3 sub(final kdVector3 v)
    {
        this.m_intrdata = this.m_intrdata.sub(v.m_intrdata);
        return this;
    }
    public kdVector3 sub(final kdVector3 v, kdVector3 dest) {return dest.set(this).sub(v);}

    public kdVector3 mul(final float cf)
    {
        this.m_intrdata = this.m_intrdata.mul(cf);
        return this;
    }
    public kdVector3 mul(final float cf, kdVector3 dest) {return dest.set(this).mul(cf);}

    public kdVector3 div(final float f)
    {
        this.m_intrdata = this.m_intrdata.div(f);
        return this;
    }
    public kdVector3 div(final float f, kdVector3 dest) {return dest.set(this).div(f);}

    public float dot(final kdVector3 v)
    {
        return this.m_intrdata.mul(v.m_intrdata).reduceLanes(VectorOperators.ADD);
    }

    public kdVector3 cross(final kdVector3 v)
    {
        FloatVector right_op = v.m_intrdata.rearrange(VecSwizzle128f_1203);
        right_op = right_op.mul(this.m_intrdata).sub(this.m_intrdata.rearrange(VecSwizzle128f_1203).mul(v.m_intrdata));
        this.m_intrdata = right_op.rearrange(VecSwizzle128f_1203);
        return this;
    }
    public kdVector3 cross(final kdVector3 v, kdVector3 dest) {return dest.set(this).cross(v);}

    public float length2()
    {
        return this.dot(this);
    }

    public float length()
    {
        return kdSqrt(this.length2());
    }

    @Override
    public boolean equals(final Object obj)
    {
        return obj instanceof final kdVector3 v && (v == this || this.m_intrdata.eq(v.m_intrdata).allTrue());
    }

    public kdVector3 normalize()
    {
        FloatVector sp = this.m_intrdata.mul(this.m_intrdata);
        FloatVector shfd = sp.rearrange(VecSwizzle128f_1212);
        FloatVector sums = sp.add(shfd);
        sums = sums.add(shfd.rearrange(VecSwizzle128f_1111));
        sums = sums.rearrange(VecSwizzle128f_0000);
        this.m_intrdata = this.m_intrdata.mul(Vec128f_one.div(sums.sqrt()));
        return this;
    }
    public kdVector3 normalize(kdVector3 dest) {return dest.set(this).normalize();}

    public kdVector3 absolute()
    {
        this.m_intrdata = this.m_intrdata.viewAsIntegralLanes().lanewise(VectorOperators.AND_NOT, Vec128f_nzero.viewAsIntegralLanes()).viewAsFloatingLanes();
        return this;
    }
    public kdVector3 absolute(kdVector3 dest) {return dest.set(this).absolute();}

    public kdVector3 rotate(final kdQuaternion rotation)
    {
        final kdQuaternion result = rotation.conjuguate(new kdQuaternion()).multiply(new kdQuaternion(this.m_intrdata));
        this.m_intrdata = result.multiply(rotation).intrinsics().withLane(3, 0.f);
        return this;
    }
    public kdVector3 rotate(final kdQuaternion rotation, kdVector3 dest) {return dest.set(this).rotate(rotation);}

    @Override
    public String toString()
    {
        return String.format("kdVector3[x=%f;y=%f;z=%f]", this.x(), this.y(), this.z());
    }
}
