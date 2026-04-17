package reobf.vmtweak.main;

/**
 * Interface injected into {@code MTEVoidMinerBase} via mixin to expose
 * dimension-override accessors to other mixins and code at runtime.
 * <p>
 * Since mixin classes themselves are not loadable at runtime, this interface
 * serves as the public contract for accessing the injected fields.
 */
public interface IVMTweakOverride {

    /**
     * @return the current warning message, or empty string if no warning.
     */
    String vmtweak$getWarning();

    /**
     * @return a human-readable description of the current dimension override for GUI display.
     */
    String vmtweak$getOverrideDisplayText();

    /**
     * @return a monotonically increasing counter that increments each time the dimension override changes.
     */
    int vmtweak$getDimChangeVersion();
}
