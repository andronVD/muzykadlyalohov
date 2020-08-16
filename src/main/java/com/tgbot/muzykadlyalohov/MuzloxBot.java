package com.tgbot.muzykadlyalohov;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;

import javax.annotation.PostConstruct;

import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.quality.AudioQuality;

@Component
public class MuzloxBot extends TelegramLongPollingBot {
	
	private static final Logger logger = LoggerFactory.getLogger(MuzloxBot.class);
	private static final Pattern p = Pattern.compile("^vi/|v=|/v/|youtu.be/|embed/$");
	
	@Value("${bot.token}")
	private String token;
	
	@Value("${bot.username}")
	private String username;

	@Value("${app.version}")
	private String appVersion;
	
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
            	createResponseMessage(message);
            } else {
            	message = update.getMessage();
            	if (message != null && message.hasText()) {
            		createResponseMessage(message);
                }
            }
        } catch (Exception e) {
            logger.info("Something went wrong: " + e.getMessage());
        }
	}
	
	private void createResponseMessage(Message message) {
		if (message.getText().contains("youtu")) {
			logger.info("!!!Start processing youtube link - " + message.getText());
			processYoutube(message);
			logger.info("!!!End processing youtube link - " + message.getText());
		} if (message.getText().contains("коваль")) {
			sendTextMessage(message, "Я люблю тебя, коваль! ты мой шедевр!");
		} if (message.getText().contains("/version")) {
			sendTextMessage(message, appVersion);
		}
	}
	
	private void processYoutube(Message message) {
		String videoId = getYoutubeVideoId(message.getText());
		YoutubeVideo video = getYoutubeVideo(videoId);
		if (video != null) {
			SendAudio res = new SendAudio();
			res.setChatId(message.getChatId());
			try {
				String audioUrl = video.audioFormats().stream()
						.filter(a -> AudioQuality.low == a.audioQuality())
						.findFirst()
						.map(AudioFormat::url)
						.orElse(null);
				URLConnection urlConnection = new URL(audioUrl).openConnection();
				String path = System.getProperty("user.dir") + "/" + video.details().title() + ".mp3";
				File faudio = new File(path);
				faudio.deleteOnExit();
				FileUtils.copyURLToFile(new URL(audioUrl), faudio);
				res.setAudio(faudio);
				execute(res);
			} catch (Exception e) {
				logger.info("Something went wrong: ", e);
			}
		}
	}

	private String getYoutubeVideoId(String messageText) {
		String videoId = messageText.split(p.pattern())[1];
		int ampersandIndex = videoId.indexOf('&');
		if (ampersandIndex != -1) {
			videoId = videoId.substring(0, ampersandIndex);
		}
		return videoId;
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
			logger.info(e.getLocalizedMessage());
		}
		return null;
	}

	private YoutubeVideo getYoutubeVideo(String videoId) {
		logger.info("Video id - " + videoId);
		YoutubeDownloader downloader = new YoutubeDownloader();

		downloader.addCipherFunctionPattern(2, "\\b([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)");
		// extractor features
		downloader.setParserRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
		downloader.setParserRetryOnFailure(1);

		YoutubeVideo video = null;
		try {
			video = downloader.getVideo(videoId);
		} catch (YoutubeException | IOException e) {
			logger.info(e.getLocalizedMessage());
		}

		return video;
	}

	private void sendTextMessage(Message message, String text) {
		SendMessage response = new SendMessage();
		Long chatId = message.getChatId();
		response.setChatId(chatId);
		response.setText(text);
		try {
			execute(response);
		} catch (TelegramApiException e) {
            logger.info("Response execution failed: " + e.getMessage());
    	}
	}

	@PostConstruct
	public void start() {
		logger.info("username: {}, token: {}", username, token);
	}


}
