package showoff.App.Render;

import kcud.ContraptionNalgebra.kdMatrix4;
import kcud.ContraptionNalgebra.kdVector3;

import java.nio.FloatBuffer;

import static kcud.ContraptionNalgebra.kdMathDefs.*;

public class Camera
{
    private static final float gMinCameraPitch = 0.005f, gMaxCameraPitch = KCUD_PI - gMinCameraPitch;

    private final kdMatrix4 m_projectionMatrix = new kdMatrix4();
    private final kdMatrix4 m_viewMatrix = new kdMatrix4();
    private final kdVector3 m_target = new kdVector3();
    private float m_distance, m_yRotation, m_xRotation;

    private final kdVector3 m_defaultTarget = new kdVector3();
    private final float m_defaultDistance, m_defaultYRotation, m_defaultXRotation;

    public Camera(kdVector3 target, kdVector3 position)
    {
        kdVector3 rel = position.sub(target, new kdVector3());
        this.m_defaultDistance = rel.length();
        this.m_defaultYRotation = kdAcos(rel.y() / this.m_defaultDistance);
        this.m_defaultXRotation = kdAtan2(rel.z(), rel.x());
        this.m_defaultTarget.set(target);
    }

    public void setProjection(float fov, float aspectRatio, float near, float far, boolean close)
    {
        final float f = 1.f / kdTan(fov * 0.5f);
        final float g = 1.f / (near - far);
        this.m_projectionMatrix.zero()
                .c00(f / aspectRatio)
                .c11(f)
                .c22((close ? far : far + near) * g)
                .c23(-1.f)
                .c32((close ? far : 2.f * far) * g * near);
    }

    public void getProjection(FloatBuffer buffer)
    {
        this.m_projectionMatrix.get(buffer);
    }

    public void updateViewMatrix()
    {
        final float sy = kdSin(this.m_yRotation);
        final kdVector3 dir = new kdVector3(
                sy * kdCos(this.m_xRotation),
                kdCos(this.m_yRotation),
                sy * kdSin(this.m_xRotation)
        );
        final kdVector3 eye = dir.mul(this.m_distance, new kdVector3()).add(this.m_target);
        final kdVector3 left = new kdVector3(0.f, 1.f, 0.f).cross(dir).normalize();
        final kdVector3 up = dir.cross(left, new kdVector3());
        this.m_viewMatrix
                .c00(left.x())
                .c01(up.x())
                .c02(dir.x())
                .c03(0.f)
                .c10(left.y())
                .c11(up.y())
                .c12(dir.y())
                .c13(0.f)
                .c20(left.z())
                .c21(up.z())
                .c22(dir.z())
                .c23(0.f)
                .c30(-left.dot(eye))
                .c31(-up.dot(eye))
                .c32(-dir.dot(eye))
                .c33(1.f);
    }

    public void reset()
    {
        this.m_target.set(this.m_defaultTarget);
        this.m_distance = this.m_defaultDistance;
        this.m_xRotation = this.m_defaultXRotation;
        this.m_yRotation = this.m_defaultYRotation;
    }

    public void addDistanceOffset(float offset)
    {
        this.m_distance = kdClamp(this.m_distance * kdPow(1.1f, offset), 0.1f, 500.f);
    }

    public void rotateView(float x_offset, float y_offset)
    {
        this.m_xRotation = (this.m_xRotation + x_offset) % KCUD_2PI;
        this.m_yRotation = kdClamp(this.m_yRotation + y_offset, gMinCameraPitch, gMaxCameraPitch);
    }

    public void moveTarget(float x_offset, float y_offset)
    {
        final float sy = kdSin(this.m_yRotation);
        final kdVector3 dir = new kdVector3(
                sy * kdCos(this.m_xRotation),
                kdCos(this.m_yRotation),
                sy * kdSin(this.m_xRotation)
        );
        kdVector3 right = dir.cross(new kdVector3(0.f, 1.f, 0.f), new kdVector3()).normalize();
        kdVector3 up = right.cross(dir, new kdVector3()).normalize();
        this.m_target.add(right.mul(x_offset).add(up.mul(y_offset)).mul(this.m_distance));
    }

    public void rotateTarget(float x_offset, float y_offset)
    {
    }

    public void getModelView(FloatBuffer buffer, kdMatrix4 model)
    {
        this.m_viewMatrix.multiply(model, new kdMatrix4()).get(buffer);
    }
}
