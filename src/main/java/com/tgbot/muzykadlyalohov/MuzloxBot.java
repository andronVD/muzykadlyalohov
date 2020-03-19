package com.tgbot.muzykadlyalohov;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;

import javax.activation.MimeType;
import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.quality.AudioQuality;

@Component
public class MuzloxBot extends TelegramLongPollingBot {
	
	private static final Logger logger = LoggerFactory.getLogger(MuzloxBot.class);
	private static final Pattern p = Pattern.compile("^vi/|v=|/v/|youtu.be/|embed/$");
	
	public enum MusicSource {
		YOUTUBE("youtu"),
		SPOTIFY("spotify"),
		SOUNDCLOUD("soundcloud"),
		APPLE_MUSIC("music.apple");
		
		private String displayUrl;
		
		MusicSource(String displayUrl) {
			this.displayUrl = displayUrl; 
		}
		
		
	}
	
	@Value("${bot.token}")
	private String token;
	
	@Value("${bot.username}")
	private String username;
	@Override
	public String getBotUsername() {
		return username;
	}

	@Override
	public String getBotToken() {
		return token;
	}

	@Override
	public void onUpdateReceived(Update update) {
        try {
            Message message = update.getChannelPost();
            if (message != null && message.hasText()) {
                try {
                    handleIncomingMessage(message);
                } catch (InvalidObjectException e) {
                    logger.debug("InvalidObjectException: " + e.getMessage());
                }
            } else {
            	message = update.getMessage();
            	if (message != null && message.hasText()) {
                    try {
                        handleIncomingMessage(message);
                    } catch (InvalidObjectException e) {
                        logger.debug("InvalidObjectException: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Something went wrong: " + e.getMessage());
        }
	}
	
	private void handleIncomingMessage(Message message) throws InvalidObjectException {
		if (message.getText().contains("youtu")) {
			executeYoutubeResponse(message);
		} if (message.getText().contains("коваль")) {
			executeResponse(message);
		}
	}
	
	private void executeYoutubeResponse(Message message) {
		String videoId = message.getText().split(p.pattern())[1];
		YoutubeVideo video = getVideo(videoId);
		if (video != null) {
			SendAudio res = new SendAudio();
			res.setChatId(message.getChatId());
			try {
				String audioUrl = video.audioFormats().stream().filter(a -> AudioQuality.low == a.audioQuality()).collect(Collectors.toList()).get(0).url();
				URL source = new URL(audioUrl);
				String path = System.getProperty("java.io.tmpdir") + video.details().title() + ".mp3"; 
				File faudio = new File(path);
				faudio.deleteOnExit();
				FileUtils.copyURLToFile(source, faudio);
				//File result = compressAudioFile(faudio);
				res.setAudio(faudio);
				execute(res);
			} catch (TelegramApiException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	private File compressAudioFile(File faudio) {
		
		try (FileInputStream fis=new FileInputStream(faudio);
				FileOutputStream fos=new FileOutputStream("file2"); 
				DeflaterOutputStream dos=new DeflaterOutputStream(fos)) {

	        int data;
	        while ((data=fis.read())!=-1)
	        {
	            dos.write(data);
	        }
	        //return new File
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private YoutubeVideo getVideo(String videoId) {
		YoutubeDownloader downloader = new YoutubeDownloader();

		downloader.addCipherFunctionPattern(2, "\\b([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)");
		// extractor features
		downloader.setParserRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
		downloader.setParserRetryOnFailure(1);

		YoutubeVideo video = null;
		try {
			video = downloader.getVideo(videoId);
		} catch (YoutubeException | IOException e) {
			e.printStackTrace();
		}

		return video;
	}

	private void executeResponse(Message message) {
		SendMessage response = new SendMessage();
		Long chatId = message.getChatId();
		response.setChatId(chatId);
		response.setText("Я люблю тебя, коваль! ты мой шедевр!");
		try {
			execute(response);
		} catch (TelegramApiException e) {
            logger.debug("Response execution failed: " + e.getMessage());
    	}
	}

	@PostConstruct
	public void start() {
		logger.info("username: {}, token: {}", username, token);
	}


}