package showoff.App.Render;

import showoff.App.ObjectMesh;

public interface SceneRenderer
{
    void attachInterface();

    int registerPipeline();
    int registerMesh(ObjectMesh mesh);
    int addInstancingGroup(int mesh_identifier, int max_object_count);

    void destroy();
}
