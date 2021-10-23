package de.jeisfeld.breathtraining.exercise;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import de.jeisfeld.breathtraining.MainActivity;
import de.jeisfeld.breathtraining.R;
import de.jeisfeld.breathtraining.sound.MediaPlayer;
import de.jeisfeld.breathtraining.sound.MediaTrigger;
import de.jeisfeld.breathtraining.ui.training.ServiceReceiver;

/**
 * A service handling Exercises in the background.
 */
public class ExerciseService extends Service {
	/**
	 * The id for the service.
	 */
	private static final int SERVICE_ID = 1;
	/**
	 * The request code for the main notification.
	 */
	private static final int REQUEST_CODE_START_APP = 1;
	/**
	 * Intent key for the service command.
	 */
	private static final String EXTRA_SERVICE_COMMAND = "de.jeisfeld.breathtraining.SERVICE_COMMAND";
	/**
	 * The id of the notification channel.
	 */
	public static final String CHANNEL_ID = "BreathTrainingChannel";
	/**
	 * The wait duration at the end, before closing.
	 */
	private static final long END_WAIT_DURATION = 2000;
	/**
	 * Delay time to allow sound pre-preparation.
	 */
	private static final long SOUND_PREPARE_DELAY = 100;

	/**
	 * The running threads.
	 */
	private final List<ExerciseAnimationThread> mRunningThreads = new ArrayList<>();
	/**
	 * Flag indicating if exercise is pausing.
	 */
	private boolean mIsPausing = false;
	/**
	 * Flag indicating if exercise is pausing.
	 */
	private boolean mIsStopping = false;
	/**
	 * Flag indicating if exercise is skipping to next breath.
	 */
	private boolean mIsSkipping = false;
	/**
	 * The current exercise step.
	 */
	private ExerciseStep mExerciseStep = null;
	/**
	 * The service query receiver.
	 */
	private ServiceQueryReceiver mServiceQueryReceiver;

	/**
	 * Trigger the exercise service.
	 *
	 * @param context        The context.
	 * @param serviceCommand The service command.
	 * @param exerciseData   The exercise data.
	 */
	public static void triggerExerciseService(final Context context, final ServiceCommand serviceCommand, final ExerciseData exerciseData) {
		Intent serviceIntent = new Intent(context, ExerciseService.class);
		serviceIntent.putExtra(EXTRA_SERVICE_COMMAND, serviceCommand);
		exerciseData.addToIntent(serviceIntent);
		ContextCompat.startForegroundService(context, serviceIntent);
	}

	@Override
	public final void onCreate() {
		super.onCreate();
		mServiceQueryReceiver = new ServiceQueryReceiver(this);
		registerReceiver(mServiceQueryReceiver, new IntentFilter(ServiceQueryReceiver.RECEIVER_ACTION));
		createNotificationChannel();
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (intent == null) {
			return START_REDELIVER_INTENT;
		}

		final ServiceCommand serviceCommand = (ServiceCommand) intent.getSerializableExtra(EXTRA_SERVICE_COMMAND);
		final ExerciseData exerciseData = ExerciseData.fromIntent(intent);
		assert exerciseData != null;
		startNotification(exerciseData, mExerciseStep, serviceCommand);

		switch (serviceCommand) {
		case START:
			ExerciseAnimationThread newThread = new ExerciseAnimationThread(exerciseData);

			synchronized (mRunningThreads) {
				mIsPausing = false;
				mIsSkipping = false;
				mIsStopping = false;
				mRunningThreads.notifyAll();
				if (mRunningThreads.size() > 0) {
					mRunningThreads.get(mRunningThreads.size() - 1).interrupt();
				}
				mRunningThreads.add(newThread);
			}
			newThread.start();
			return START_STICKY;
		case STOP:
			synchronized (mRunningThreads) {
				mIsPausing = false;
				mIsSkipping = false;
				mIsStopping = true;
				mRunningThreads.notifyAll();
				if (mRunningThreads.size() > 0) {
					mRunningThreads.get(mRunningThreads.size() - 1).interrupt();
				}
			}
			return START_STICKY;
		case PAUSE:
			synchronized (mRunningThreads) {
				mIsPausing = true;
				mIsSkipping = true;
				if (mRunningThreads.size() > 0) {
					mRunningThreads.get(mRunningThreads.size() - 1).interrupt();
					mRunningThreads.get(mRunningThreads.size() - 1).updateExerciseData(exerciseData);
				}
			}
			return START_STICKY;
		case RESUME:
			synchronized (mRunningThreads) {
				if (mRunningThreads.size() > 0) {
					mRunningThreads.get(mRunningThreads.size() - 1).updateExerciseData(exerciseData);
				}
				mIsPausing = false;
				mRunningThreads.notifyAll();
			}
			return START_STICKY;
		case SKIP:
			synchronized (mRunningThreads) {
				if (mRunningThreads.size() > 0) {
					mIsSkipping = true;
					mRunningThreads.get(mRunningThreads.size() - 1).interrupt();
				}
			}
			return START_STICKY;
		default:
			return START_STICKY;
		}
	}

	@Override
	public final void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mServiceQueryReceiver);
	}

	@Override
	public final IBinder onBind(final Intent intent) {
		return null;
	}

	/**
	 * Get a wakelock and acquire it.
	 *
	 * @param thread The thread aquiring the wakelock.
	 * @return The wakelock.
	 */
	@SuppressLint("WakelockTimeout")
	private WakeLock acquireWakelock(final Thread thread) {
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		assert powerManager != null;
		WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.jeisfeld.breathtraining:" + thread.hashCode());
		wakelock.acquire();
		return wakelock;
	}

	/**
	 * Create the channel for service animation notifications.
	 */
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel animationChannel = new NotificationChannel(
					CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager manager = getSystemService(NotificationManager.class);
			assert manager != null;
			manager.createNotificationChannel(animationChannel);
		}
	}

	/**
	 * Start the notification.
	 *
	 * @param exerciseData   The exercise data.
	 * @param exerciseStep   The current exercise step.
	 * @param serviceCommand The service command.
	 */
	private void startNotification(final ExerciseData exerciseData, final ExerciseStep exerciseStep, final ServiceCommand serviceCommand) {
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		exerciseData.addToIntent(notificationIntent);
		notificationIntent.putExtra(ServiceReceiver.EXTRA_EXERCISE_STEP, exerciseStep);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				REQUEST_CODE_START_APP, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

		int contentTextResource = R.string.notification_text_exercise_running;
		if (exerciseStep != null) {
			contentTextResource = exerciseStep.getStepType().getDisplayResource();
		}
		else if (serviceCommand != null && serviceCommand.getDisplayResource() != 0) {
			contentTextResource = serviceCommand.getDisplayResource();
		}

		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title_exercise))
				.setContentText(getString(contentTextResource))
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.ic_notification)
				.build();
		startForeground(SERVICE_ID, notification);
	}

	/**
	 * Update the service after the exercise has ended.
	 *
	 * @param wakeLock      The wakelock.
	 * @param animationData The instance of animationData which is ended.
	 * @param thread        The thread which is ended.
	 */
	private void updateOnEndExercise(final WakeLock wakeLock, final ExerciseData animationData, final Thread thread) {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		synchronized (mRunningThreads) {
			// noinspection SuspiciousMethodCalls
			mRunningThreads.remove(thread);
			if (mRunningThreads.size() == 0) {
				MediaPlayer.releaseInstance(MediaTrigger.SERVICE);
				stopService(new Intent(this, ExerciseService.class));
				sendBroadcast(ServiceReceiver.createIntent(PlayStatus.STOPPED, null));
			}
		}
	}

	/**
	 * A command to be triggered on the service.
	 */
	public enum ServiceCommand {
		/**
		 * Start.
		 */
		START(R.string.text_starting),
		/**
		 * Stop.
		 */
		STOP(R.string.text_stopping),
		/**
		 * Pause.
		 */
		PAUSE(R.string.text_pausing),
		/**
		 * Resume.
		 */
		RESUME(R.string.text_resuming),
		/**
		 * Skip to next step.
		 */
		SKIP(R.string.text_skipping);

		/**
		 * The text resource for displaying the service command.
		 */
		private final int mTextResource;

		/**
		 * Constructor.
		 *
		 * @param textResource The text resource for displaying the service command.
		 */
		ServiceCommand(final int textResource) {
			mTextResource = textResource;
		}

		/**
		 * Get the String resource for display.
		 *
		 * @return The string resource.
		 */
		public int getDisplayResource() {
			return mTextResource;
		}
	}

	/**
	 * An animation thread for the exercise.
	 */
	private final class ExerciseAnimationThread extends Thread {
		/**
		 * the exercise data.
		 */
		private ExerciseData mExerciseData;

		/**
		 * Constructor.
		 *
		 * @param exerciseData The exercise data.
		 */
		private ExerciseAnimationThread(final ExerciseData exerciseData) {
			mExerciseData = exerciseData;
		}

		/**
		 * Update the exercise data.
		 *
		 * @param exerciseData The new exercise data.
		 */
		private void updateExerciseData(final ExerciseData exerciseData) {
			exerciseData.retrieveStatus(mExerciseData);
			mExerciseData = exerciseData;
		}

		@Override
		public void run() {
			final WakeLock wakeLock = acquireWakelock(this);
			long nextDelay = SOUND_PREPARE_DELAY;

			mExerciseStep = mExerciseData.getNextStep();
			while (mExerciseStep != null) {
				if (!(mIsSkipping && mExerciseStep.getStepType() == StepType.HOLD)) {
					// Execute the step, except in case of hold while skipping
					mIsSkipping = false;
					MediaPlayer.getInstance().play(ExerciseService.this, MediaTrigger.SERVICE,
							mExerciseData.getSoundType(), mExerciseStep.getStepType(), nextDelay);
					sendBroadcast(ServiceReceiver.createIntent(null, mExerciseStep));
					startNotification(mExerciseData, mExerciseStep, null);
					try {
						if (mExerciseStep.getDuration() > SOUND_PREPARE_DELAY) {
							// noinspection BusyWait
							Thread.sleep(mExerciseStep.getDuration() - SOUND_PREPARE_DELAY);
							nextDelay = SOUND_PREPARE_DELAY;
						}
						else {
							nextDelay = mExerciseStep.getDuration();
						}
					}
					catch (InterruptedException e) {
						if (mIsStopping) {
							updateOnEndExercise(wakeLock, mExerciseData, this);
							return;
						}
					}
					synchronized (mRunningThreads) {
						if (mIsPausing) {
							try {
								mRunningThreads.wait();
							}
							catch (InterruptedException e) {
								if (mIsStopping) {
									updateOnEndExercise(wakeLock, mExerciseData, this);
									return;
								}
							}
						}
					}
				}
				mExerciseStep = mExerciseData.getNextStep();
			}

			try {
				MediaPlayer.getInstance().play(ExerciseService.this, MediaTrigger.SERVICE, mExerciseData.getSoundType(),
						StepType.RELAX, nextDelay);
				mExerciseStep = new ExerciseStep(StepType.RELAX, 0, 0);
				sendBroadcast(ServiceReceiver.createIntent(PlayStatus.PLAYING, mExerciseStep));
				startNotification(mExerciseData, mExerciseStep, null);
				Thread.sleep(END_WAIT_DURATION);
			}
			catch (InterruptedException e) {
				// Ignore
			}
			updateOnEndExercise(wakeLock, mExerciseData, this);
		}
	}

	/**
	 * A broadcast receiver for receiving messages from service to update UI.
	 */
	public static class ServiceQueryReceiver extends BroadcastReceiver {
		/**
		 * The action triggering this receiver.
		 */
		public static final String RECEIVER_ACTION = "de.jeisfeld.breathtraining.SERVICE_QUERY_RECEIVER";

		private final WeakReference<ExerciseService> mExerciseService;

		/**
		 * Default constructor.
		 */
		public ServiceQueryReceiver() {
			mExerciseService = null;
		}

		/**
		 * Constructor.
		 * @param exerciseService The exerciseService.
		 */
		public ServiceQueryReceiver(final ExerciseService exerciseService) {
			mExerciseService = new WeakReference<>(exerciseService);
		}

		@Override
		public final void onReceive(final Context context, final Intent intent) {
			if (mExerciseService == null) {
				return;
			}
			ExerciseService exerciseService = mExerciseService.get();
			if (exerciseService != null) {
				synchronized (exerciseService.mRunningThreads) {
					if(exerciseService.mRunningThreads.size() > 0) {
						ExerciseData exerciseData = exerciseService.mRunningThreads.get(exerciseService.mRunningThreads.size() - 1).mExerciseData;
						Intent serviceIntent = ServiceReceiver.createIntent(exerciseData.getPlayStatus(), exerciseService.mExerciseStep);
						exerciseData.addToIntent(serviceIntent);
						exerciseService.sendBroadcast(serviceIntent);
					}
				}
			}
		}
	}
}
