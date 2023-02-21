package showoff.App.Feature;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record OBJModel(Map<String, Mesh> meshes)
{
    public record Component2(float x, float y) {}
    public record Component3(float x, float y, float z) {}
    public record Face(int v0, int v1, int v2) {}

    public record Mesh(String name, Component3[] vertices, Component3[] normals, Component2[] uvs, Face[] faces) {}

    public OBJModel(List<Mesh> meshes)
    {
        this(meshes.stream().collect(Collectors.toMap(Mesh::name, Function.identity())));
    }
}
