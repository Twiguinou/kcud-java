package showoff.App.Feature;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record OBJModel(Map<String, Mesh> meshes, MaterialLibrary[] materials)
{
    public record Component2(float x, float y) {}
    public record Component3(float x, float y, float z) {}
    public record Component4(float x, float y, float z, float w) {}
    public record Face(int v0, int v1, int v2) {}

    public record MaterialLibrary(Component4 tint)
    {
        public boolean opaque()
        {
            return this.tint.w == 1.f;
        }
    }

    public record Mesh(String name, Component3[] vertices, Component3[] normals, Component2[] uvs, Face[] faces) {}

    public OBJModel(Collection<Mesh> meshes, MaterialLibrary[] materials)
    {
        this(meshes.stream().collect(Collectors.toMap(Mesh::name, Function.identity())), materials);
    }
}
