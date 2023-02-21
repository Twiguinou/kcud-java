package showoff.App;

import showoff.App.Render.SceneRenderer;
import showoff.App.Render.VulkanSceneRenderer;

import java.util.function.Supplier;

public enum RenderBackend
{
    VULKAN(VulkanSceneRenderer::new);

    public final Supplier<SceneRenderer> renderer;
    RenderBackend(Supplier<SceneRenderer> renderer)
    {
        this.renderer = renderer;
    }
}
