import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Role {
    private final String id;
    private String name;
    private String description;
    private final Set<Permission> permissions;


    public Role(String name, String description) {
        this.id = "role_" + UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.permissions = ConcurrentHashMap.newKeySet();
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }


    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }


    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        for (Permission p : permissions) {
            if (p.name().equalsIgnoreCase(permissionName) &&
                    p.resource().equalsIgnoreCase(resource)) {
                return true;
            }
        }
        return false;
    }


    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description));
        sb.append(String.format("Permissions (%d):\n", permissions.size()));

        if (permissions.isEmpty()) {
            sb.append("  - No permissions\n");
        } else {
            for (Permission p : permissions) {
                sb.append("  - ").append(p.format()).append("\n");
            }
        }

        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id.equals(role.id);
    }


    @Override
    public int hashCode() {
        return id.hashCode();
    }


    @Override
    public String toString() {
        String shortId = id.length() > 8 ? id.substring(0, 8) + "..." : id;
        return String.format("Role{id='%s', name='%s'}", shortId, name);
    }
}