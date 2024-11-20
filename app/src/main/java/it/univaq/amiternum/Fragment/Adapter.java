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

    private final List<Oggetto3D> oggetti;

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

        private TextView playerPosition, playerPosition2;
        private SeekBar seekBar, seekBar2;
        private ImageView btPlay, btPlay2;
        private ImageView btPause, btPause2;

        private MediaPlayer mediaPlayer;
        private final Handler handler = new Handler();
        private Runnable runnable, runnable2;


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
            TextView playerDuration = artwork.findViewById(R.id.player_duration);
            seekBar = artwork.findViewById(R.id.seek_bar);
            ImageView btRew = artwork.findViewById(R.id.bt_rew);
            btPlay = artwork.findViewById(R.id.bt_play);
            btPause = artwork.findViewById(R.id.bt_pause);
            ImageView btFw = artwork.findViewById(R.id.bt_fw);

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
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
                mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    btPause.setVisibility(View.GONE);
                    btPlay.setVisibility(View.VISIBLE);
                    mediaPlayer.seekTo(0);
                });
            } else {
                artwork.findViewById(R.id.audioLayout).setVisibility(View.GONE);
            }

            playerPosition2 = artwork.findViewById(R.id.player_position2);
            TextView playerDuration2 = artwork.findViewById(R.id.player_duration2);
            seekBar2 = artwork.findViewById(R.id.seek_bar2);
            ImageView btRew2 = artwork.findViewById(R.id.bt_rew2);
            btPlay2 = artwork.findViewById(R.id.bt_play2);
            btPause2 = artwork.findViewById(R.id.bt_pause2);
            ImageView btFw2 = artwork.findViewById(R.id.bt_fw2);

            if(!oggetto.getUrlVideo().isEmpty()) {
                VideoView video = artwork.findViewById(R.id.videoView);
                video.setVideoURI(Uri.parse(oggetto.getUrlVideo()));
                runnable2 = () -> {
                    if(video.isPlaying()) {
                        seekBar2.setProgress(video.getCurrentPosition());
                        playerPosition2.setText(convertFormat(video.getCurrentPosition()));
                        seekBar2.postDelayed(runnable2, 500);
                    }
                };
                playerPosition2.setText(R.string.playerZero);
                btPlay2.setOnClickListener(view1 -> {
                    btPlay2.setVisibility(View.GONE);
                    btPause2.setVisibility(View.VISIBLE);
                    seekBar2.setMax(video.getDuration());
                    video.start();
                    video.postDelayed(runnable2, 0);
                });
                btPause2.setOnClickListener(view1 -> {
                    btPlay2.setVisibility(View.VISIBLE);
                    btPause2.setVisibility(View.GONE);
                    video.pause();
                    video.removeCallbacks(runnable2);
                });
                btFw2.setOnClickListener(view1 -> {
                    int currentPosition = video.getCurrentPosition();
                    int fwDuration = video.getDuration();
                    if (fwDuration != currentPosition) {
                        currentPosition = Math.min(currentPosition + 5000, fwDuration);
                        playerPosition2.setText(convertFormat(currentPosition));
                        video.seekTo(currentPosition);
                        seekBar2.postDelayed(() -> seekBar2.setProgress(video.getCurrentPosition()), 100);
                    }
                });
                btRew2.setOnClickListener(view1 -> {
                    int currentPosition = video.getCurrentPosition();
                    if (currentPosition > 5000)
                        currentPosition = currentPosition - 5000;
                    else
                        currentPosition = 0;
                    playerPosition2.setText(convertFormat(currentPosition));
                    video.seekTo(currentPosition);
                    seekBar2.postDelayed(() -> seekBar2.setProgress(video.getCurrentPosition()), 100);
                });
                video.setOnPreparedListener(mediaPlayer1 -> {
                    playerDuration2.setText(convertFormat(video.getDuration()));
                    seekBar2.postDelayed(runnable2, 0);
                });
                seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser)
                            video.seekTo(progress);
                        playerPosition2.setText(convertFormat(video.getCurrentPosition()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        video.pause();
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if(btPause2.getVisibility() == View.VISIBLE)
                            video.start();
                    }
                });
                video.setOnCompletionListener(mediaPlayer -> {
                    btPause2.setVisibility(View.GONE);
                    btPlay2.setVisibility(View.VISIBLE);
                    video.seekTo(0);
                    seekBar2.setProgress(0);
                });

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
