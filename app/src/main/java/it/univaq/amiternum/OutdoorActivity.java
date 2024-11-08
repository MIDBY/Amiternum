package it.univaq.amiternum;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;

public class OutdoorActivity extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_outdoor, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView image = view.findViewById(R.id.main_divider_img);
        image.setOnTouchListener((view2, motionEvent) -> {
            int height;
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    height = view.findViewById(R.id.ARContainer).getHeight();
                    Guideline guideLine = view.findViewById(R.id.divider);
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                    params.guidePercent = (motionEvent.getRawY()) / height;
                    if(0.1 < params.guidePercent && params.guidePercent < 0.8)
                        guideLine.setLayoutParams(params);
                    else
                        return false;
                    break;
                default:
                    return false;
            }
            return true;
        });
    }
}