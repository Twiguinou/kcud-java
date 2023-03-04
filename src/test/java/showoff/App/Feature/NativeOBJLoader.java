package showoff.App.Feature;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import showoff.FrameAllocator;

import java.io.File;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.assimp.Assimp.*;

public class NativeOBJLoader extends OBJModelLoader
{
    public NativeOBJLoader(Settings settings)
    {
        super(settings);
    }

    @Override
    public OBJModel parse(File file) throws Error
    {
        int flags = aiProcess_GenNormals | aiProcess_GenSmoothNormals | aiProcess_CalcTangentSpace | aiProcess_ImproveCacheLocality | aiProcess_OptimizeMeshes | aiProcess_FixInfacingNormals
                | aiProcess_FindDegenerates | aiProcess_FindInvalidData | aiProcess_GenUVCoords | aiProcess_TransformUVCoords | aiProcess_JoinIdenticalVertices;
        if (this.m_settings.triangulate()) flags |= aiProcess_Triangulate;
        if (this.m_settings.flip_uvs()) flags |= aiProcess_FlipUVs;
        AIScene scene = aiImportFile(file.getAbsolutePath(), flags);
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null)
        {
            throw new Error("Failed to load model using Assimp : " + aiGetErrorString());
        }

        OBJModel.Mesh[] meshes = new OBJModel.Mesh[scene.mNumMeshes()];
        PointerBuffer pMeshes = scene.mMeshes();

        OBJModel.MaterialLibrary[] materials = new OBJModel.MaterialLibrary[scene.mNumMaterials()];
        PointerBuffer pMaterials = scene.mMaterials();
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            AIColor4D color = AIColor4D.malloc(allocator);
            for (int i = 0; i < materials.length; i++)
            {
                AIMaterial material = AIMaterial.create(pMaterials.get(i));
                aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_DIFFUSE, 0, color);
                materials[i] = new OBJModel.MaterialLibrary(new OBJModel.Component4(color.r(), color.g(), color.b(), color.a()));
            }
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor())
        {
            for (int i = 0; i <  pMeshes.capacity(); i++)
            {
                final int fi = i;
                executor.submit(() ->
                {
                    final AIMesh mesh = AIMesh.create(pMeshes.get(fi));
                    OBJModel.Component3[] vertices = mesh.mVertices().stream().map(vertex -> new OBJModel.Component3(vertex.x(), vertex.y(), vertex.z())).toArray(OBJModel.Component3[]::new);
                    OBJModel.Component3[] normals = mesh.mNormals().stream().map(normal -> new OBJModel.Component3(normal.x(), normal.y(), normal.z())).toArray(OBJModel.Component3[]::new);
                    AIVector3D.Buffer pUvs = mesh.mTextureCoords(0);
                    OBJModel.Component2[] uvs = new OBJModel.Component2[pUvs == null ? 0 : pUvs.capacity()];
                    for (int j = 0; j < uvs.length; j++)
                    {
                        AIVector3D uv = pUvs.get(j);
                        uvs[j] = new OBJModel.Component2(uv.x(), uv.y());
                    }
                    OBJModel.Face[] faces = mesh.mFaces().stream().filter(face -> face.mNumIndices() == 3).map(face ->
                    {
                        final IntBuffer idx = face.mIndices();
                        return new OBJModel.Face(idx.get(0), idx.get(1), idx.get(2));
                    }).toArray(OBJModel.Face[]::new);

                    meshes[fi] = new OBJModel.Mesh(mesh.mName().dataString(), vertices, normals, uvs, faces);
                });
            }
        }

        aiReleaseImport(scene);
        return new OBJModel(Arrays.stream(meshes).collect(Collectors.toUnmodifiableMap(OBJModel.Mesh::name, Function.identity())), materials);
    }
}
