package showoff.App.Feature;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class NativeOBJLoader extends OBJModelLoader
{
    public NativeOBJLoader(Settings settings)
    {
        super(settings);
    }

    @Override
    public OBJModel parse(InputStream input) throws Error
    {
        try (MemorySession bloat_session = MemorySession.openConfined())
        {
            ByteBuffer main_file_data;
            try
            {
                final byte[] bytes = input.readAllBytes();
                main_file_data = bloat_session.allocateArray(ValueLayout.JAVA_BYTE, bytes.length).asByteBuffer().put(0, bytes);
            }
            catch (IOException e)
            {
                throw new Error(e);
            }

            int flags = aiProcess_GenNormals | aiProcess_GenSmoothNormals | aiProcess_CalcTangentSpace | aiProcess_ImproveCacheLocality | aiProcess_OptimizeMeshes | aiProcess_FixInfacingNormals
                    | aiProcess_FindDegenerates | aiProcess_FindInvalidData | aiProcess_GenUVCoords | aiProcess_TransformUVCoords | aiProcess_JoinIdenticalVertices;
            if (this.m_settings.triangulate()) flags |= aiProcess_Triangulate;
            if (this.m_settings.flip_uvs()) flags |= aiProcess_FlipUVs;
            AIScene scene = aiImportFileFromMemory(main_file_data, flags, (ByteBuffer)null);
            if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null)
            {
                throw new Error("Failed to load model using Assimp : " + aiGetErrorString());
            }

            List<OBJModel.Mesh> meshes = new LinkedList<>();
            PointerBuffer pMeshes = scene.mMeshes();
            for (int i = 0; i <  pMeshes.capacity(); i++)
            {
                final AIMesh mesh = AIMesh.create(pMeshes.get(i));
                OBJModel.Component3[] vertices = mesh.mVertices().stream().map(vertex -> new OBJModel.Component3(vertex.x(), vertex.y(), vertex.z())).toArray(OBJModel.Component3[]::new);
                OBJModel.Component3[] normals = mesh.mNormals().stream().map(normal -> new OBJModel.Component3(normal.x(), normal.y(), normal.z())).toArray(OBJModel.Component3[]::new);
                OBJModel.Face[] faces = mesh.mFaces().stream().filter(face -> face.mNumIndices() == 3).map(face ->
                {
                    final IntBuffer idx = face.mIndices();
                    return new OBJModel.Face(idx.get(0), idx.get(1), idx.get(2));
                }).toArray(OBJModel.Face[]::new);

                meshes.add(new OBJModel.Mesh(mesh.mName().dataString(), vertices, normals, null, faces));
            }

            return new OBJModel(meshes);
        }
    }
}
