import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Object assignmentLock = new Object();
    private final Map<String, RoleAssignment> assignmentsById = new ConcurrentHashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        User user = assignment.user();
        Role role = assignment.role();

        synchronized (assignmentLock) {
            if (!userManager.exists(user.username())) {
                throw new IllegalArgumentException("User '" + user.username() + "' does not exist");
            }

            if (!roleManager.exists(role.getName())) {
                throw new IllegalArgumentException("Role '" + role.getName() + "' does not exist");
            }

            if (hasActiveAssignment(user, role)) {
                throw new IllegalStateException("User already has active assignment for role '" + role.getName() + "'");
            }

            assignmentsById.put(assignment.assignmentId(), assignment);
        }
    }

    private boolean hasActiveAssignment(User user, Role role) {
        for (RoleAssignment a : assignmentsById.values()) {
            if (a.user().equals(user) && a.role().equals(role) && a.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        return assignmentsById.remove(assignment.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignmentsById.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignmentsById.values());
    }

    @Override
    public int count() {
        return assignmentsById.size();
    }

    @Override
    public void clear() {
        assignmentsById.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return new ArrayList<>();
        }

        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment a : assignmentsById.values()) {
            if (a.user().equals(user)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return new ArrayList<>();
        }

        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment a : assignmentsById.values()) {
            if (a.role().equals(role)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }

        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment a : assignmentsById.values()) {
            if (filter.test(a)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return assignmentsById.values().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        List<RoleAssignment> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public List<RoleAssignment> getActiveAssignments() {
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment a : assignmentsById.values()) {
            if (a.isActive()) {
                result.add(a);
            }
        }
        return result;
    }

    public List<RoleAssignment> getExpiredAssignments() {
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment a : assignmentsById.values()) {
            if (!a.isActive()) {
                result.add(a);
            }
        }
        return result;
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }

        for (RoleAssignment a : assignmentsById.values()) {
            if (a.user().equals(user) && a.role().equals(role) && a.isActive()) {
                return true;
            }
        }
        return false;
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }

        Set<Permission> userPermissions = getUserPermissions(user);
        for (Permission p : userPermissions) {
            if (p.name().equalsIgnoreCase(permissionName) && p.resource().equalsIgnoreCase(resource)) {
                return true;
            }
        }
        return false;
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return new HashSet<>();
        }

        Set<Permission> permissions = new HashSet<>();
        for (RoleAssignment a : assignmentsById.values()) {
            if (a.user().equals(user) && a.isActive()) {
                permissions.addAll(a.role().getPermissions());
            }
        }
        return permissions;
    }

    public void revokeAssignment(String assignmentId) {
        ValidationUtils.requireNonEmpty(assignmentId, "Assignment ID");

        synchronized (assignmentLock) {
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment not found with id: " + assignmentId);
            }

            if (assignment instanceof PermanentAssignment) {
                ((PermanentAssignment) assignment).revoke();
            } else {
                throw new IllegalArgumentException("Only permanent assignments can be revoked");
            }
        }
    }

    /**
     * Помечает истёкшие временные назначения как неактивные (планировщик).
     * Снимок списка делается за один проход по копии — без длительной блокировки карты.
     */
    public int deactivateExpiredTemporaryByScheduler() {
        int marked = 0;
        for (RoleAssignment a : new ArrayList<>(assignmentsById.values())) {
            if (a instanceof TemporaryAssignment temp && temp.markInactiveBySchedulerIfExpired()) {
                marked++;
            }
        }
        return marked;
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        ValidationUtils.requireNonEmpty(assignmentId, "Assignment ID");
        ValidationUtils.requireNonEmpty(newExpirationDate, "New expiration date");
        newExpirationDate = ValidationUtils.normalizeString(newExpirationDate);
        if (!ValidationUtils.isValidDate(newExpirationDate)) {
            throw new IllegalArgumentException("New expiration date должен быть в формате yyyy-MM-dd HH:mm");
        }

        synchronized (assignmentLock) {
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment not found with id: " + assignmentId);
            }

            if (assignment instanceof TemporaryAssignment) {
                ((TemporaryAssignment) assignment).extend(newExpirationDate);
            } else {
                throw new IllegalArgumentException("Only temporary assignments can be extended");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentManager that = (AssignmentManager) o;
        return assignmentsById.equals(that.assignmentsById);
    }

    @Override
    public int hashCode() {
        return assignmentsById.hashCode();
    }

    @Override
    public String toString() {
        return String.format("AssignmentManager{assignments=%d}", assignmentsById.size());
    }
}
