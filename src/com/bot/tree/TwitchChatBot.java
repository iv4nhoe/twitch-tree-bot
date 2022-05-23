package com.bot.tree;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jibble.pircbot.*;
import org.yaml.snakeyaml.*;

public class TwitchChatBot extends PircBot {
	private String token;
	private String botName;
	private String[] channels;

	private static ArrayList<Tree> trees = new ArrayList<Tree>();
	private LocalDateTime lastMessage = LocalDateTime.now().minusHours(42);

	private Weather weather = new Weather();

	public boolean init(String token, String botName, String[] channels) {
		this.token = token;
		this.botName = botName;
		this.channels = channels.clone();

		loadTrees();

		return connect();
	}

	private void loadTrees() {
		Yaml yaml = new Yaml();
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("trees.yaml");
		Map<String, Object> treesYaml = yaml.load(inputStream);
		for (Map.Entry<String, Object> entry : treesYaml.entrySet()) {
			trees.add(loadTree(entry));
		}
	}

	private Tree loadTree(Map.Entry<String, Object> treeYaml) {
		String latinName = treeYaml.getKey();
			Object englishObject = ((LinkedHashMap<String, Object>) treeYaml.getValue()).get("en");
			ArrayList<String> englishNames = parseTreeNames(englishObject);
			Object germanObject = ((LinkedHashMap<String, Object>) treeYaml.getValue()).get("de");
			ArrayList<String> germanNames = parseTreeNames(germanObject);
			Object swedishObject = ((LinkedHashMap<String, Object>) treeYaml.getValue()).get("se");
			ArrayList<String> swedishNames = parseTreeNames(swedishObject);
			Tree t = new Tree();
			t.setLatinName(latinName);
			t.setEnglishNames(englishNames);
			t.setGermanNames(germanNames);
			t.setSwedishNames(swedishNames);
			System.out.println(t.getDescription());
			return t;
	}

	private ArrayList<String> parseTreeNames(Object nameObject) {
		ArrayList<String> names;
		if(nameObject.getClass() == (new ArrayList<String>().getClass())) {
			names = (ArrayList<String>) nameObject;
		} else {
			names = new ArrayList<String>();
			names.add((String) nameObject);
		}
		return names;
	}

	public boolean connect() {
		try {
			connect("irc.twitch.tv", 6667, token);
		} catch (NickAlreadyInUseException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (IrcException e) {
			e.printStackTrace();
			return false;
		}
		for (String channel : channels) {
			joinChannel("#" + channel);
			System.out.println("Connected to: " + channel);
		}

		return true;
	}

	@Override
	protected void onDisconnect() {
		super.onDisconnect();
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connect();
	}

	public void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		if(LocalDateTime.now().minusSeconds(30).isAfter(lastMessage))
		{
			Tree t = containsTreeName(message);
			if(t != null) {
				sendMessage(channel, "🤖 🌳🌲 I overheard you talking about a tree! " + t.getDescription());
				updateLastMessageTimestamp();
			}
			if(sender.equalsIgnoreCase(botName)) {
				return;
			}
			String msg = message.toLowerCase();
			if(msg.contains("pripps")) {
				sendMessage(channel, "ArbPripps You cannot translate glorious Pripps Blå ArbPripps ");
			}
			else if(msg.startsWith("!weather")) {
				sendMessage(channel, "🤖 " + weather.toReadableString());
			}
			else if(msg.startsWith("!estimate")) {
				sendMessage(channel, "🤖 " + new Estimate(msg).toString());
			}
			else if(msg.startsWith("!chipper")) {
				sendMessage(channel, "🤖 We have got two chippers. One trusty Timberwolf and an always broken Jensen.");
			}
			else if(msg.startsWith("!chainsaw")) {
				sendMessage(channel, "🤖 We have enough chainsaws.");
			}
			else if(msg.startsWith("!commands") || msg.startsWith("!help")) {
				sendMessage(channel, "🤖 You can use the command !estimate to get an estimate on a tree job. Use !weather to get the current weather in Uppsala. Mention any tree name in a chat message and I will tell you how the tree is called in different languages. Use !chipper to get info about the chippers. Use !chainsaw to get info about chainsaws.");
			}
		}
	}

	private Tree containsTreeName(String str) {
		for (Tree tree : trees) {
			for (String name : tree.getAllNames()) {
				Pattern regex = Pattern.compile("(^|\\W)" + name.toLowerCase() + "($|\\W)");
				Matcher regexMatcher = regex.matcher(str.toLowerCase());
				if(regexMatcher.find()) {
					return tree;
				}
			}
		}
		return null;
	}

	private void updateLastMessageTimestamp() {
		lastMessage = LocalDateTime.now();
	}
}
