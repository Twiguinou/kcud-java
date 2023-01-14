package kcud.ContraptionNalgebra;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShuffle;

public class kdVector3
{

    private FloatVector m_intrdata;

    public kdVector3()
    {
        this.m_intrdata = FloatVector.zero(FloatVector.SPECIES_128);
    }

    public kdVector3(float x, float y, float z)
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

    public void zero()
    {
        this.m_intrdata = __kdVector3_zero128;
    }

    public float x() {return this.m_intrdata.lane(0);}
    public void x(float x) {this.m_intrdata = this.m_intrdata.withLane(0, x);}
    public float y() {return this.m_intrdata.lane(1);}
    public void y(float y) {this.m_intrdata = this.m_intrdata.withLane(1, y);}
    public float z() {return this.m_intrdata.lane(2);}
    public void z(float z) {this.m_intrdata = this.m_intrdata.withLane(2, z);}

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

    public kdVector3 sub(final kdVector3 v)
    {
        this.m_intrdata = this.m_intrdata.sub(v.m_intrdata);
        return this;
    }

    public kdVector3 mul(float cf)
    {
        this.m_intrdata = this.m_intrdata.mul(cf);
        return this;
    }

    public kdVector3 div(float f)
    {
        this.m_intrdata = this.m_intrdata.div(f);
        return this;
    }

    public float dot(final kdVector3 v)
    {
        return this.m_intrdata.mul(v.m_intrdata).reduceLanes(VectorOperators.ADD);
    }

    public kdVector3 cross(final kdVector3 v)
    {
        FloatVector right_op = v.m_intrdata.rearrange(__kdVector3_shuffle1203);
        right_op = right_op.mul(this.m_intrdata).sub(this.m_intrdata.rearrange(__kdVector3_shuffle1203).mul(v.m_intrdata));
        return new kdVector3(right_op.rearrange(__kdVector3_shuffle1203));
    }

    public float length2()
    {
        return this.dot(this);
    }

    public float length()
    {
        return (float)Math.sqrt(this.length2());
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof final kdVector3 v && this.m_intrdata.eq(v.m_intrdata).allTrue();
    }

    public kdVector3 normalize()
    {
        FloatVector sp = this.m_intrdata.mul(this.m_intrdata);
        FloatVector shfd = sp.rearrange(__kdVector3_shuffle1212);
        FloatVector sums = sp.add(shfd);
        sums = sums.add(shfd.rearrange(__kdVector3_shuffle1111));
        sums = sums.rearrange(__kdVector3_shuffle0000);
        this.m_intrdata = this.m_intrdata.mul(__kdVector3_unary128.div(sums.sqrt()));
        return this;
    }

    public kdVector3 absolute()
    {
        return new kdVector3(this.m_intrdata.viewAsIntegralLanes().lanewise(VectorOperators.AND_NOT, __kdVector3_nzero128).viewAsFloatingLanes());
    }

    public kdVector3 normalized()
    {
        return new kdVector3(this.m_intrdata).normalize();
    }

    @Override
    public String toString()
    {
        return String.format("kdVector3[x=%f;y=%f;z=%f]", this.x(), this.y(), this.z());
    }

    private static final VectorShuffle<Float> __kdVector3_shuffle1203 = FloatVector.SPECIES_128.shuffleFromValues(1, 2, 0, 3);
    private static final VectorShuffle<Float> __kdVector3_shuffle1212 = FloatVector.SPECIES_128.shuffleFromValues(1, 2, 1, 2);
    private static final VectorShuffle<Float> __kdVector3_shuffle1111 = FloatVector.SPECIES_128.shuffleFromValues(1, 1, 1, 1);
    private static final VectorShuffle<Float> __kdVector3_shuffle0000 = FloatVector.SPECIES_128.shuffleFromValues(0, 0, 0, 0);
    private static final FloatVector __kdVector3_unary128 = FloatVector.broadcast(FloatVector.SPECIES_128, 1.f);
    private static final IntVector __kdVector3_nzero128 = FloatVector.broadcast(FloatVector.SPECIES_128, -0.f).viewAsIntegralLanes();
    private static final FloatVector __kdVector3_zero128 = FloatVector.zero(FloatVector.SPECIES_128);
}
