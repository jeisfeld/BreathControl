package de.jeisfeld.breathcontrol.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import de.jeisfeld.breathcontrol.exercise.ExerciseStep;
import de.jeisfeld.breathcontrol.exercise.PlayStatus;

/**
 * A broadcast receiver for receiving messages from service to update UI.
 */
public class ServiceReceiver extends BroadcastReceiver {
	/**
	 * The action triggering this receiver.
	 */
	public static final String RECEIVER_ACTION = "de.jeisfeld.breathcontrol.SERVICE_RECEIVER";
	/**
	 * Key for the play status.
	 */
	public static final String EXTRA_PLAY_STATUS = "de.jeisfeld.breathcontrol.PLAY_STATUS";
	/**
	 * Key for the exercise step.
	 */
	public static final String EXTRA_EXERCISE_STEP = "de.jeisfeld.breathcontrol.EXERCISE_STEP";
	/**
	 * Handler used to execute code on the UI thread.
	 */
	private final Handler mHandler;
	/**
	 * The view model.
	 */
	private final HomeViewModel mHomeViewModel;

	/**
	 * Create a broadcast intent to send the playStatus to this receiver.
	 *
	 * @param playStatus The play status.
	 * @return The intent.
	 */
	public static Intent createIntent(final PlayStatus playStatus) {
		Intent intent = new Intent(RECEIVER_ACTION);
		intent.putExtra(EXTRA_PLAY_STATUS, playStatus);
		return intent;
	}

	/**
	 * Create a broadcast intent to send the exercise step to this receiver.
	 *
	 * @param exerciseStep The exercise step.
	 * @return The intent.
	 */
	public static Intent createIntent(final ExerciseStep exerciseStep) {
		Intent intent = new Intent(RECEIVER_ACTION);
		intent.putExtra(EXTRA_EXERCISE_STEP, exerciseStep);
		return intent;
	}

	/**
	 * Default Constructor.
	 */
	public ServiceReceiver() {
		mHandler = null;
		mHomeViewModel = null;
	}

	/**
	 * Constructor.
	 *
	 * @param handler The handler.
	 * @param homeViewModel The UI model.
	 */
	public ServiceReceiver(final Handler handler, final HomeViewModel homeViewModel) {
		mHandler = handler;
		mHomeViewModel = homeViewModel;
	}

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		if (mHandler == null || mHomeViewModel == null) {
			return;
		}

		PlayStatus playStatus = (PlayStatus) intent.getSerializableExtra(EXTRA_PLAY_STATUS);
		if (playStatus != null) {
			mHandler.post(() -> mHomeViewModel.updatePlayStatus(playStatus));
		}

		ExerciseStep exerciseStep = (ExerciseStep) intent.getSerializableExtra(EXTRA_EXERCISE_STEP);
		if (exerciseStep != null) {
			mHandler.post(() -> mHomeViewModel.updateExerciseStep(exerciseStep));
		}
	}
}
