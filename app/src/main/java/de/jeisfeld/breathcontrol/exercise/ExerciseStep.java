package de.jeisfeld.breathcontrol.exercise;

/**
 * An exercise step.
 */
public class ExerciseStep {
	/**
	 * The step type.
	 */
	private final StepType mStepType;
	/**
	 * The step duration.
	 */
	private final long mDuration;

	/**
	 * Constructor.
	 *
	 * @param stepType The step type.
	 * @param duration The duration.
	 */
	public ExerciseStep(final StepType stepType, final long duration) {
		mStepType = stepType;
		mDuration = duration;
	}

	/**
	 * Get the step type.
	 *
	 * @return The step type.
	 */
	public StepType getStepType() {
		return mStepType;
	}

	/**
	 * Get the duration.
	 *
	 * @return The duration.
	 */
	public long getDuration() {
		return mDuration;
	}

}
