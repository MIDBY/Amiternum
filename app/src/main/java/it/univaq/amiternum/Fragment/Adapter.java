package it.univaq.amiternum.Fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.concurrent.TimeUnit;

import it.univaq.amiternum.Model.Oggetto3D;
import it.univaq.amiternum.R;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private List<Oggetto3D> oggetti;

    public Adapter(List<Oggetto3D> oggetti) {
        this.oggetti = oggetti;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_artwork, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.onbind(oggetti.get(position));
    }

    @Override
    public int getItemCount() {
        return oggetti != null ? oggetti.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView title;
        private final ImageView image;

        private TextView playerPosition, playerDuration;
        private SeekBar seekBar;
        private ImageView btRew, btPlay, btPause, btFw;

        private MediaPlayer mediaPlayer;
        private Handler handler = new Handler();
        private Runnable runnable;


        public ViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.textTitleArtwork);
            image = view.findViewById(R.id.imageArtwork);
            view.findViewById(R.id.layoutRoot).setOnClickListener(this);
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        @Override
        public void onClick(View view) {
            Oggetto3D oggetto = oggetti.get(getAdapterPosition());
            Dialog artwork = new Dialog(view.getContext());
            artwork.setContentView(R.layout.dialog_artwork);

            ((TextView) artwork.findViewById(R.id.artworkTitle)).setText(oggetto.getNome());
            String description = itemView.getContext().getString(R.string.descriptionText) + oggetto.getDescrizione();
            ((TextView) artwork.findViewById(R.id.artworkDescription)).setText(description);
            ((TextView) artwork.findViewById(R.id.artworkAudioResource)).setText(R.string.audioResourceText);
            ((TextView) artwork.findViewById(R.id.artworkVideoResource)).setText(R.string.videoResourceText);

            playerPosition = artwork.findViewById(R.id.player_position);
            playerDuration = artwork.findViewById(R.id.player_duration);
            seekBar = artwork.findViewById(R.id.seek_bar);
            btRew = artwork.findViewById(R.id.bt_rew);
            btPlay = artwork.findViewById(R.id.bt_play);
            btPause = artwork.findViewById(R.id.bt_pause);
            btFw = artwork.findViewById(R.id.bt_fw);

            if(!oggetto.getUrlAudio().isEmpty()) {
                Uri uri = Uri.parse(oggetto.getUrlAudio());
                mediaPlayer = MediaPlayer.create(artwork.getContext(), uri);
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        handler.postDelayed(this, 500);
                    }
                };
                int duration = mediaPlayer.getDuration();
                String sDuration = convertFormat(duration);
                playerPosition.setText(R.string.playerZero);
                playerDuration.setText(sDuration);
                btPlay.setOnClickListener(view1 -> {
                    btPlay.setVisibility(View.GONE);
                    btPause.setVisibility(View.VISIBLE);
                    mediaPlayer.start();
                    seekBar.setMax(mediaPlayer.getDuration());
                    handler.postDelayed(runnable, 0);
                });
                btPause.setOnClickListener(view1 -> {
                    btPlay.setVisibility(View.VISIBLE);
                    btPause.setVisibility(View.GONE);
                    mediaPlayer.pause();
                    handler.removeCallbacks(runnable);
                });
                btFw.setOnClickListener(view1 -> {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int fwDuration = mediaPlayer.getDuration();
                    if (fwDuration != currentPosition) {
                        currentPosition = Math.min(currentPosition + 5000, fwDuration);
                        playerPosition.setText(convertFormat(currentPosition));
                        mediaPlayer.seekTo(currentPosition);
                    }
                });
                btRew.setOnClickListener(view1 -> {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    if (currentPosition > 5000)
                        currentPosition = currentPosition - 5000;
                    else
                        currentPosition = 0;
                    playerPosition.setText(convertFormat(currentPosition));
                    mediaPlayer.seekTo(currentPosition);
                });
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            mediaPlayer.seekTo(progress);
                        }
                        playerPosition.setText(convertFormat(mediaPlayer.getCurrentPosition()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    btPause.setVisibility(View.GONE);
                    btPlay.setVisibility(View.VISIBLE);
                    mediaPlayer.seekTo(0);
                });
            } else {
                artwork.findViewById(R.id.audioLayout).setVisibility(View.GONE);
            }

            if(!oggetto.getUrlVideo().isEmpty()) {
                VideoView video = artwork.findViewById(R.id.videoView);
                video.setVideoPath(oggetto.getUrlVideo());
                MediaController mediaController = new MediaController(video.getContext());
                video.setMediaController(mediaController);
                mediaController.setAnchorView(video);
            } else {
                artwork.findViewById(R.id.artworkVideoResource).setVisibility(View.GONE);
                artwork.findViewById(R.id.videoView).setVisibility(View.GONE);
            }

            artwork.findViewById(R.id.indoorArButton).setOnClickListener(v -> {
                artwork.dismiss();
                Navigation.findNavController(view).navigate(R.id.action_navHome_to_navIndoor);
            });

            artwork.show();
            artwork.setCanceledOnTouchOutside(true);
        }

        public void onbind(Oggetto3D oggetto) {
            title.setText(oggetto.getNome());
            if(oggetto.getUrlFiles() != null) {
                Picasso.get().load(oggetto.getFirstUrlFileByExtension("jpg")).resize(180,160).centerCrop().into(image);
            }
        }

        @SuppressLint("DefaultLocale")
        private String convertFormat(int duration) {
            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
        }
    }
}
