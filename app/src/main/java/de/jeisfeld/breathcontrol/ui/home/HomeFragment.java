package de.jeisfeld.breathcontrol.ui.home;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import de.jeisfeld.breathcontrol.R;
import de.jeisfeld.breathcontrol.databinding.FragmentHomeBinding;
import de.jeisfeld.breathcontrol.ui.home.HomeViewModel.Mode;

/**
 * The fragment for managing basic breath control page.
 */
public class HomeFragment extends Fragment {
	/**
	 * The number of milliseconds per second.
	 */
	private static final double MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);
	/**
	 * The max number of milliseconds displayed as two-digit seconds with decimal point.
	 */
	private static final int MAX_TWODIGIT_SECONDS = 99900;

	/**
	 * The view model.
	 */
	private HomeViewModel mHomeViewModel;
	/**
	 * The fragment binding.
	 */
	private FragmentHomeBinding mBinding;

	@Override
	public final View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mHomeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
		mBinding = FragmentHomeBinding.inflate(inflater, container, false);
		View root = mBinding.getRoot();

		final Spinner spinnerMode = mBinding.spinnerMode;
		spinnerMode.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.spinner_item_mode, getModeSpinnerValues()));
		mHomeViewModel.getMode().observe(getViewLifecycleOwner(), mode -> spinnerMode.setSelection(mode.ordinal()));
		spinnerMode.setOnItemSelectedListener(getOnModeSelectedListener(root, mHomeViewModel));

		final Spinner spinnerHoldPosition = mBinding.spinnerHoldPosition;
		spinnerHoldPosition.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.spinner_item_hold_position, getHoldPositionSpinnerValues()));
		mHomeViewModel.getHoldPosition().observe(getViewLifecycleOwner(), holdPosition -> spinnerHoldPosition.setSelection(holdPosition.ordinal()));

		prepareSeekbarRepetitions(root);
		prepareSeekbarBreathDuration(root);
		prepareSeekbarBreathEndDuration(root);
		prepareSeekbarHoldStartDuration(root);
		prepareSeekbarHoldEndDuration(root);
		prepareSeekbarInOutRelation(root);
		prepareSeekbarHoldVariation(root);

		return root;
	}

	@Override
	public final void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}

	/**
	 * Prepare the repetitions seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarRepetitions(final View root) {
		mBinding.textViewRepetitions.setText(String.format(Locale.getDefault(), "%d",
				HomeViewModel.repetitionsSeekbarToValue(mBinding.seekBarRepetitions.getProgress())));
		mHomeViewModel.getRepetitions().observe(getViewLifecycleOwner(), value -> {
			int seekBarValue = HomeViewModel.repetitionsValueToSeekbar(value);
			mBinding.seekBarRepetitions.setProgress(seekBarValue);
			mBinding.textViewRepetitions.setText(String.format(Locale.getDefault(), "%d", value));
		});
		mBinding.seekBarRepetitions.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress ->
						mHomeViewModel.updateRepetitions(HomeViewModel.repetitionsSeekbarToValue(progress)));
	}

	/**
	 * Prepare the breath duration seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarBreathDuration(final View root) {
		long breathDuration = HomeViewModel.durationSeekbarToValue(mBinding.seekBarBreathDuration.getProgress(), false);
		if (breathDuration >= MAX_TWODIGIT_SECONDS) {
			mBinding.textViewBreathDuration.setText(String.format(Locale.getDefault(), "%.0fs", breathDuration / MILLIS_PER_SECOND));
		}
		else {
			mBinding.textViewBreathDuration.setText(String.format(Locale.getDefault(), "%.1fs", breathDuration / MILLIS_PER_SECOND));
		}
		mHomeViewModel.getBreathDuration().observe(getViewLifecycleOwner(), duration -> {
			int seekbarValue = HomeViewModel.durationValueToSeekbar(duration);
			mBinding.seekBarBreathDuration.setProgress(seekbarValue);
			if (duration >= MAX_TWODIGIT_SECONDS) {
				mBinding.textViewBreathDuration.setText(String.format(Locale.getDefault(), "%.0fs", duration / MILLIS_PER_SECOND));
			}
			else {
				mBinding.textViewBreathDuration.setText(String.format(Locale.getDefault(), "%.1fs", duration / MILLIS_PER_SECOND));
			}
		});
		mBinding.seekBarBreathDuration.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress ->
						mHomeViewModel.updateBreathDuration(HomeViewModel.durationSeekbarToValue(progress, false)));
	}

	/**
	 * Prepare the breath end duration seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarBreathEndDuration(final View root) {
		long breathEndDuration = HomeViewModel.durationSeekbarToValue(mBinding.seekBarBreathEndDuration.getProgress(), false);
		if (breathEndDuration >= MAX_TWODIGIT_SECONDS) {
			mBinding.textViewBreathEndDuration.setText(String.format(Locale.getDefault(), "%.0fs", breathEndDuration / MILLIS_PER_SECOND));
		}
		else {
			mBinding.textViewBreathEndDuration.setText(String.format(Locale.getDefault(), "%.1fs", breathEndDuration / MILLIS_PER_SECOND));
		}
		mHomeViewModel.getBreathEndDuration().observe(getViewLifecycleOwner(), duration -> {
			int seekbarValue = HomeViewModel.durationValueToSeekbar(duration);
			mBinding.seekBarBreathEndDuration.setProgress(seekbarValue);
			if (duration >= MAX_TWODIGIT_SECONDS) {
				mBinding.textViewBreathEndDuration.setText(String.format(Locale.getDefault(), "%.0fs", duration / MILLIS_PER_SECOND));
			}
			else {
				mBinding.textViewBreathEndDuration.setText(String.format(Locale.getDefault(), "%.1fs", duration / MILLIS_PER_SECOND));
			}
		});
		mBinding.seekBarBreathEndDuration.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress ->
						mHomeViewModel.updateBreathEndDuration(HomeViewModel.durationSeekbarToValue(progress, false)));
	}

	/**
	 * Prepare the hold start duration seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarHoldStartDuration(final View root) {
		long holdStartDuration = HomeViewModel.durationSeekbarToValue(mBinding.seekBarHoldStartDuration.getProgress(), true);
		if (holdStartDuration >= MAX_TWODIGIT_SECONDS) {
			mBinding.textViewHoldStartDuration.setText(String.format(Locale.getDefault(), "%.0fs", holdStartDuration / MILLIS_PER_SECOND));
		}
		else {
			mBinding.textViewHoldStartDuration.setText(String.format(Locale.getDefault(), "%.1fs", holdStartDuration / MILLIS_PER_SECOND));
		}
		mHomeViewModel.getHoldStartDuration().observe(getViewLifecycleOwner(), duration -> {
			int seekbarValue = HomeViewModel.durationValueToSeekbar(duration);
			mBinding.seekBarHoldStartDuration.setProgress(seekbarValue);
			if (duration >= MAX_TWODIGIT_SECONDS) {
				mBinding.textViewHoldStartDuration.setText(String.format(Locale.getDefault(), "%.0fs", duration / MILLIS_PER_SECOND));
			}
			else {
				mBinding.textViewHoldStartDuration.setText(String.format(Locale.getDefault(), "%.1fs", duration / MILLIS_PER_SECOND));
			}
		});
		mBinding.seekBarHoldStartDuration.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress ->
						mHomeViewModel.updateHoldStartDuration(HomeViewModel.durationSeekbarToValue(progress, true)));
	}

	/**
	 * Prepare the hold end duration seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarHoldEndDuration(final View root) {
		long holdEndDuration = HomeViewModel.durationSeekbarToValue(mBinding.seekBarHoldEndDuration.getProgress(), true);
		if (holdEndDuration >= MAX_TWODIGIT_SECONDS) {
			mBinding.textViewHoldEndDuration.setText(String.format(Locale.getDefault(), "%.0fs", holdEndDuration / MILLIS_PER_SECOND));
		}
		else {
			mBinding.textViewHoldEndDuration.setText(String.format(Locale.getDefault(), "%.1fs", holdEndDuration / MILLIS_PER_SECOND));
		}
		mHomeViewModel.getHoldEndDuration().observe(getViewLifecycleOwner(), duration -> {
			int seekbarValue = HomeViewModel.durationValueToSeekbar(duration);
			mBinding.seekBarHoldEndDuration.setProgress(seekbarValue);
			if (duration >= MAX_TWODIGIT_SECONDS) {
				mBinding.textViewHoldEndDuration.setText(String.format(Locale.getDefault(), "%.0fs", duration / MILLIS_PER_SECOND));
			}
			else {
				mBinding.textViewHoldEndDuration.setText(String.format(Locale.getDefault(), "%.1fs", duration / MILLIS_PER_SECOND));
			}
		});
		mBinding.seekBarHoldEndDuration.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress ->
						mHomeViewModel.updateHoldEndDuration(HomeViewModel.durationSeekbarToValue(progress, true)));
	}

	/**
	 * Prepare the in out relation seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarInOutRelation(final View root) {
		mBinding.textViewInOutRelation.setText(String.format(Locale.getDefault(), "%d%%", mBinding.seekBarInOutRelation.getProgress()));
		mHomeViewModel.getInOutRelation().observe(getViewLifecycleOwner(), value -> {
			int seekBarValue = (int) Math.round(value * 100); // MAGIC_NUMBER
			mBinding.seekBarInOutRelation.setProgress(seekBarValue);
			mBinding.textViewInOutRelation.setText(String.format(Locale.getDefault(), "%d%%", seekBarValue));
		});
		mBinding.seekBarInOutRelation.setOnSeekBarChangeListener(
				(OnSeekBarProgressChangedListener) progress -> {
					mHomeViewModel.updateInOutRelation(progress / 100.0); // MAGIC_NUMBER
				});
	}

	/**
	 * Prepare the hold variation seekbar.
	 *
	 * @param root The parent view.
	 */
	private void prepareSeekbarHoldVariation(final View root) {
		mBinding.textViewHoldVariation.setText(String.format(Locale.getDefault(), "%d%%", mBinding.seekBarHoldVariation.getProgress()));
		mHomeViewModel.getHoldVariation().observe(getViewLifecycleOwner(), value -> {
			int seekBarValue = (int) Math.round(value * 100); // MAGIC_NUMBER
			mBinding.seekBarHoldVariation.setProgress(seekBarValue);
			mBinding.textViewHoldVariation.setText(String.format(Locale.getDefault(), "%d%%", seekBarValue));
		});
		mBinding.seekBarHoldVariation.setOnSeekBarChangeListener(
				(OnSeekBarProgressChangedListener) progress -> {
					mHomeViewModel.updateHoldVariation(progress / 100.0); // MAGIC_NUMBER
				});
	}

	/**
	 * Get values of the mode spinner.
	 *
	 * @return The values of the mode spinner.
	 */
	private String[] getModeSpinnerValues() {
		return getResources().getStringArray(R.array.values_mode);
	}

	/**
	 * Get values of the hold position spinner.
	 *
	 * @return The values of the hold position spinner.
	 */
	private String[] getHoldPositionSpinnerValues() {
		return getResources().getStringArray(R.array.values_hold_position);
	}

	/**
	 * Get the listener on mode change.
	 *
	 * @param parentView The parent view.
	 * @param viewModel The view model.
	 * @return The listener.
	 */
	protected final OnItemSelectedListener getOnModeSelectedListener(final View parentView, final HomeViewModel viewModel) {
		return new OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				Mode mode = Mode.values()[position];
				switch (mode) {
				case SIMPLE:
					mBinding.tableRowBreathEndDuration.setVisibility(View.VISIBLE);
					mBinding.tableRowHoldStartDuration.setVisibility(View.GONE);
					mBinding.tableRowHoldEndDuration.setVisibility(View.GONE);
					mBinding.tableRowHoldPosition.setVisibility(View.GONE);
					mBinding.tableRowHoldVariation.setVisibility(View.GONE);
					break;
				case HOLD:
					mBinding.tableRowBreathEndDuration.setVisibility(View.GONE);
					mBinding.tableRowHoldStartDuration.setVisibility(View.VISIBLE);
					mBinding.tableRowHoldEndDuration.setVisibility(View.VISIBLE);
					mBinding.tableRowHoldPosition.setVisibility(View.VISIBLE);
					mBinding.tableRowHoldVariation.setVisibility(View.VISIBLE);
					break;
				default:
					break;
				}
				viewModel.updateMode(mode);
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// do nothing
			}
		};
	}

	/**
	 * Variant of OnSeekBarChangeListener that reacts only on progress change from user.
	 */
	private interface OnSeekBarProgressChangedListener extends OnSeekBarChangeListener {
		/**
		 * Callback on progress change from user.
		 *
		 * @param progress The progress.
		 */
		void onProgessChanged(int progress);

		@Override
		default void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
			if (fromUser) {
				onProgessChanged(progress);
			}
		}

		@Override
		default void onStartTrackingTouch(final SeekBar seekBar) {
			// do nothing
		}

		@Override
		default void onStopTrackingTouch(final SeekBar seekBar) {
			// do nothing
		}
	}

}
