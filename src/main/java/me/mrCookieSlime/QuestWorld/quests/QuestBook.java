package me.mrCookieSlime.QuestWorld.quests;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuOpeningHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.MenuHelper;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.MenuHelper.ChatHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Player.PlayerInventory;
import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.hooks.CitizensListener;
import me.mrCookieSlime.QuestWorld.listeners.Input;
import me.mrCookieSlime.QuestWorld.listeners.InputType;
import me.mrCookieSlime.QuestWorld.listeners.ChatInputCallback;
import me.mrCookieSlime.QuestWorld.utils.ItemBuilder;
import me.mrCookieSlime.QuestWorld.utils.Text;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestBook {
	
	public static void openMainMenu(Player p) {
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(null);
		
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.title"));
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().QuestClick().playTo(p);
			}
		});
		
		addPartyMenuButton(menu, p);
		
		for (final Category category: QuestWorld.getInstance().getCategories()) {
			if (!category.isHidden()) {
				if (category.isWorldEnabled(p.getWorld().getName())) {
					if ((category.getParent() != null && !QuestWorld.getInstance().getManager(p).hasFinished(category.getParent())) || !category.hasPermission(p)) {
						menu.addItem(category.getID() + 9, new CustomItem(new MaterialData(Material.BARRIER), category.getName(), "", QuestWorld.getInstance().getBookLocal("quests.locked")));
						menu.addMenuClickHandler(category.getID() + 9, new MenuClickHandler() {
							
							@Override
							public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
								return false;
							}
						});
					}
					else {
						ItemStack item = category.getItem();
						ItemMeta im = item.getItemMeta();
						List<String> lore = new ArrayList<String>();
						lore.add("");
						lore.add(category.getProgress(p));
						lore.add("");
						lore.add(Text.colorize("&7" + category.getQuests().size() + QuestWorld.getInstance().getBookLocal("category.desc.total")));
						lore.add(Text.colorize("&a" + category.getFinishedQuests(p).size() + QuestWorld.getInstance().getBookLocal("category.desc.completed")));
						lore.add(Text.colorize("&b" + category.getQuests(p, QuestStatus.AVAILABLE).size() + QuestWorld.getInstance().getBookLocal("category.desc.available")));
						lore.add(Text.colorize("&e" + category.getQuests(p, QuestStatus.ON_COOLDOWN).size() + QuestWorld.getInstance().getBookLocal("category.desc.cooldown")));
						lore.add(Text.colorize("&5" + category.getQuests(p, QuestStatus.REWARD_CLAIMABLE).size() + QuestWorld.getInstance().getBookLocal("category.desc.claimable_reward")));
						im.setLore(lore);
						item.setItemMeta(im);
						menu.addItem(category.getID() + 9, item);
						menu.addMenuClickHandler(category.getID() + 9, new MenuClickHandler() {
							
							@Override
							public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
								openCategory(p, category, true);
								return false;
							}
						});
					}
				}
				else {
					menu.addItem(category.getID() + 9, new CustomItem(new MaterialData(Material.BARRIER), category.getName(), "", QuestWorld.getInstance().getBookLocal("quests.locked-in-world")));
					menu.addMenuClickHandler(category.getID() + 9, new MenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
							return false;
						}
					});
				}
			}
		}
		menu.open(p);
	}
	
	public static void openLastMenu(Player p) {
		QWObject last = QuestWorld.getInstance().getManager(p).getLastEntry();
		if (last != null) {
			if (last instanceof Category)
				QuestBook.openCategory(p, (Category) last, true);
			
			else if (last instanceof Quest)
				QuestBook.openQuest(p, (Quest) last, true, true);
			
			return;
		}
		
		QuestBook.openMainMenu(p);
	}
	
	private static void addPartyMenuButton(ChestMenu menu, Player p) {
		if (QuestWorld.getInstance().getCfg().getBoolean("party.enabled")) {
			ItemStack skullItem = new ItemBuilder(SkullType.PLAYER)
				.display(QuestWorld.getInstance().getBookLocal("gui.party"))
				.lore(QuestWorld.getInstance().getManager(p).getProgress(), "", QuestWorld.getInstance().getBookLocal("button.open"))
				.get();
			menu.addItem(4, skullItem);
			menu.addMenuClickHandler(4, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					openPartyMenu(arg0);
					return false;
				}
			});
		}
		else {
			menu.addItem(4, new CustomItem(new MaterialData(Material.ENCHANTED_BOOK), "&eQuest Book", "", QuestWorld.getInstance().getManager(p).getProgress()));
			menu.addMenuClickHandler(4, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
		}
	}

	public static void openPartyMembers(final Player p) {
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.party"));
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().PartyClick().playTo(p);
			}
		});
		ItemBuilder skull = new ItemBuilder(SkullType.PLAYER);
		menu.addItem(4, skull.display(QuestWorld.getInstance().getBookLocal("gui.party")).lore("", QuestWorld.getInstance().getBookLocal("button.back.party")).get());
		menu.addMenuClickHandler(4, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
				openPartyMenu(arg0);
				return false;
			}
		});
		
		final Party party = QuestWorld.getInstance().getManager(p).getParty();
		if (party != null) {
			for (int i = 0; i < party.getPlayers().size(); i++) {
				final OfflinePlayer player = Bukkit.getOfflinePlayer(party.getPlayers().get(i));
				if (!party.isLeader(p)) {
					
					ItemStack item = skull.skull(player.getName()).display("&e" + player.getName()).lore("", (party.isLeader(player) ? "&4Party Leader": "&eParty Member")).get();
					menu.addItem(i + 9, item);
					menu.addMenuClickHandler(i + 9, new MenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
							return false;
						}
					});
				}
				else {
					ItemStack item = skull.skull(player.getName())
							.display("&e" + player.getName())
							.lore("", (party.isLeader(player) ? "&5&lParty Leader": "&e&lParty Member"), "", (party.isLeader(player) ? "": "&7&oClick here to kick this Member"))
							.get();
					menu.addItem(i + 9, item);
					menu.addMenuClickHandler(i + 9, new MenuClickHandler() {
						
						@Override
						public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
							if (!party.isLeader(player)) {
								party.removePlayer(player.getName());
								openPartyMembers(p);
							}
							return false;
						}
					});
				}
			}
		}
		
		menu.open(p);
	}

	public static void openPartyMenu(final Player p) {
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.party"));
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().PartyClick().playTo(p);
			}
		});
		
		menu.addItem(4, new CustomItem(new MaterialData(Material.MAP), QuestWorld.getInstance().getBookLocal("gui.title"), "", QuestWorld.getInstance().getBookLocal("button.back.quests")));
		menu.addMenuClickHandler(4, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
				openMainMenu(p);
				return false;
			}
		});
		
		final Party party = QuestWorld.getInstance().getManager(p).getParty();
		
		ItemBuilder wool = new ItemBuilder(Material.WOOL);
		
		if (party == null) {
			menu.addItem(9, wool.color(DyeColor.GREEN).display("&a&lCreate a new Party").lore("", "&rCreates a brand new Party for you", "&rto invite Friends and share your Progress").getNew());
			menu.addMenuClickHandler(9, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					Party.create(p);
					openPartyMenu(p);
					return false;
				}
			});
		}
		else {
			if (party.isLeader(p)) {
				menu.addItem(9, wool.color(DyeColor.GREEN).display("&a&lInvite a Player").lore("", "&rInvites a Player to your Party", "&rMax. Party Members: &e" + QuestWorld.getInstance().getCfg().getInt("party.max-members")).getNew());
				menu.addMenuClickHandler(9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						if (party.getPlayers().size() >= QuestWorld.getInstance().getCfg().getInt("party.max-members")) QuestWorld.getInstance().getLocalization().sendTranslation(p, "party.full", true);
						else {
							QuestWorld.getInstance().getLocalization().sendTranslation(p, "party.invite", true);
							QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.PARTY_INVITE, party));
							p.closeInventory();
						}
						return false;
					}
				});
				
				menu.addItem(17, wool.color(DyeColor.RED).display("&4&lDelete your Party").lore("", "&rDeletes this Party", "&rBe careful with this Option!").getNew());
				menu.addMenuClickHandler(17, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						party.abandon();
						openPartyMenu(p);
						return false;
					}
				});
			}
			else {
				menu.addItem(17, wool.color(DyeColor.RED).display("&4&lLeave your Party").lore("", "&rLeaves this Party", "&rBe careful with this Option!").getNew());
				menu.addMenuClickHandler(17, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						party.removePlayer(p.getName());
						openPartyMenu(p);
						return false;
					}
				});
			}
			ItemStack skullItem = new ItemBuilder(SkullType.PLAYER).display("&eMember List").lore("", "&rShows you all Members of this Party").get();
			menu.addItem(13, skullItem);
			menu.addMenuClickHandler(13, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					openPartyMembers(p);
					return false;
				}
			});
		}
		
		menu.open(p);
	}

	public static void openCategory(Player p, Category category, final boolean back) {
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(category);
		
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.title"));
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().QuestClick().playTo(p);
			}
		});
		
		addPartyMenuButton(menu, p);
		
		if (back) {
			menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), QuestWorld.getInstance().getBookLocal("button.back.general")));
			menu.addMenuClickHandler(0, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					openMainMenu(arg0);
					return false;
				}
			});
		}
		
		ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE).color(DyeColor.RED);
		
		for (final Quest quest: category.getQuests()) {
			glassPane.display(quest.getName());
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED) || !quest.isWorldEnabled(p.getWorld().getName())) {
				menu.addItem(quest.getID() + 9, glassPane.lore("", QuestWorld.getInstance().getBookLocal("quests.locked")).getNew());
				menu.addMenuClickHandler(quest.getID() + 9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED_NO_PARTY)) {
				menu.addItem(quest.getID() + 9, glassPane.lore("", "&4You need to leave your current Party").getNew());
				menu.addMenuClickHandler(quest.getID() + 9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED_PARTY_SIZE)) {
				menu.addItem(quest.getID() + 9, glassPane.lore("", "&4You can only do this Quest in a Party", "&4with at least &c" + quest.getPartySize() + " &4Members").getNew());
				menu.addMenuClickHandler(quest.getID() + 9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else {
				ItemStack item = quest.getItem();
				ItemMeta im = item.getItemMeta();
				List<String> lore = new ArrayList<String>();
				lore.add("");
				lore.add(quest.getProgress(p));
				lore.add("");
				lore.add(Text.colorize("&7") + quest.getFinishedTasks(p).size() + "/" + quest.getMissions().size() + QuestWorld.getInstance().getBookLocal("quests.tasks_completed"));
				if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
					lore.add("");
					lore.add(QuestWorld.getInstance().getBookLocal("quests.state.reward_claimable"));
				}
				else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
					lore.add("");
					lore.add(QuestWorld.getInstance().getBookLocal("quests.state.cooldown"));
				}
				else if (QuestWorld.getInstance().getManager(p).hasFinished(quest)) {
					lore.add("");
					lore.add(QuestWorld.getInstance().getBookLocal("quests.state.completed"));
				}
				im.setLore(lore);
				item.setItemMeta(im);
				menu.addItem(quest.getID() + 9, item);
				menu.addMenuClickHandler(quest.getID() + 9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						openQuest(p, quest, back, true);
						return false;
					}
				});
			}
		}
		menu.open(p);
	}
	
	public static void openQuest(final Player p, final Quest quest, final boolean categoryBack, final boolean back) {
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(quest);
		
		ChestMenu menu = new ChestMenu(QuestWorld.getInstance().getBookLocal("gui.title"));
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().QuestClick().playTo(p);
			}
		});
		
		if (back) {
			menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), QuestWorld.getInstance().getBookLocal("button.back.general")));
			menu.addMenuClickHandler(0, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					openCategory(p, quest.getCategory(), categoryBack);
					return false;
				}
			});
		}
		
		if (quest.getCooldown() > 0) {
			String cooldown = quest.getFormattedCooldown();
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
				long remaining = (QuestWorld.getInstance().getManager(p).getCooldownEnd(quest) - System.currentTimeMillis()) / 60 / 1000;
				cooldown = (remaining / 60) + "h " + (remaining % 60) + "m remaining";
			}
			menu.addItem(8, new CustomItem(new MaterialData(Material.WATCH), QuestWorld.getInstance().getBookLocal("quests.display.cooldown"), "", "&b" + cooldown));
			menu.addMenuClickHandler(8, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
		}
		
		int rewardIndex = 2;
		if (quest.getMoney() > 0 && QuestWorld.getInstance().getEconomy() != null) {
			menu.addItem(rewardIndex, new CustomItem(new MaterialData(Material.GOLD_INGOT), QuestWorld.getInstance().getBookLocal("quests.display.monetary"), "", "&6$" + quest.getMoney()));
			menu.addMenuClickHandler(rewardIndex, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
			rewardIndex++;
		}
		
		if (quest.getXP() > 0) {
			menu.addItem(rewardIndex, new CustomItem(new MaterialData(Material.EXP_BOTTLE), QuestWorld.getInstance().getBookLocal("quests.display.exp"), "", "&a" + quest.getXP() + " Level"));
			menu.addMenuClickHandler(rewardIndex, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
			rewardIndex++;
		}
		
		ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE);
		
		int index = 9;
		for (final QuestMission mission: quest.getMissions()) {
			if (QuestWorld.getInstance().getManager(p).hasUnlockedTask(mission)) {
				String manual = null;
				if (mission.getType().getID().equals("DETECT")) manual = "Detect";
				else if (mission.getType().getID().equals("SUBMIT")) manual = "Submit";
				else if (mission.getType().getID().equals("REACH_LOCATION")) manual = "Detect";
				
				ItemBuilder entryItem = new ItemBuilder(mission.getItem()).display(mission.getText());

				if (manual == null) entryItem.lore("", mission.getProgress(p));
				else entryItem.lore("", mission.getProgress(p), "", "&r> Click for Manual " + manual);
				
				menu.addItem(index, entryItem.get());
			}
			else {
				
				menu.addItem(index, glassPane.color(DyeColor.RED).display("&7&kSOMEWEIRDMISSION").lore("", QuestWorld.getInstance().getBookLocal("task.locked")).get());
			}
			
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestManager manager = QuestWorld.getInstance().getManager(p);
					
					if (!manager.hasUnlockedTask(mission)) return false;
					if (manager.getStatus(quest).equals(QuestStatus.AVAILABLE) && quest.isWorldEnabled(p.getWorld().getName())) {
						if (manager.hasCompletedTask(mission)) return false;
						if (mission.getType().getID().equals("DETECT")) {
							int amount = 0;
							for (int i = 0; i < 36; i++) {
								ItemStack current = p.getInventory().getItem(i);
								if (QuestWorld.getInstance().isItemSimiliar(mission.getItem(), current)) amount = amount + current.getAmount();
							}
							if (amount >= mission.getAmount()) manager.setProgress(mission, mission.getAmount());
							openQuest(p, quest, categoryBack, back);
						}
						else if (mission.getType().getID().equals("SUBMIT")) {
							boolean success = false;
							
							for (int i = 0; i < 36; i++) {
								ItemStack current = p.getInventory().getItem(i);
								if (QuestWorld.getInstance().isItemSimiliar(current, mission.getItem())) {
									success = true;
									int rest = manager.addProgress(mission, current.getAmount());
									if (rest > 0) {
										p.getInventory().setItem(i, new CustomItem(current, rest));
										break;
									}
									else p.getInventory().setItem(i, null);
								}
							}
							
							if(success) {
								QuestWorld.getSounds().MissionSubmit().playTo(p);
								PlayerInventory.update(p);
								QuestWorld.getSounds().muteNext();
								openQuest(p, quest, categoryBack, back);
							}
							else
								QuestWorld.getSounds().MissionReject().playTo(p);
						}
						else if (mission.getType().getID().equals("REACH_LOCATION")) {
							if (mission.getLocation().getWorld().getName().equals(p.getWorld().getName()) && mission.getLocation().distance(p.getLocation()) < mission.getAmount()) {
								QuestWorld.getInstance().getManager(p).setProgress(mission, 1);
								openQuest(p, quest, categoryBack, back);
							}
						}
					}
					return false;
				}
			});
			index++;
		}
		
		for (int i = 0; i < 9; i++) {
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
				menu.addItem(i + 18, glassPane.color(DyeColor.PURPLE).display(QuestWorld.getInstance().getBookLocal("quests.state.reward_claim")).get());
				menu.addMenuClickHandler(i + 18, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						quest.handoutReward(p);
						QuestWorld.getSounds().muteNext();
						openQuest(p, quest, categoryBack, back);
						return false;
					}
				});
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
				menu.addItem(i + 18, glassPane.color(DyeColor.YELLOW).display(QuestWorld.getInstance().getBookLocal("quests.state.cooldown")).get());
				menu.addMenuClickHandler(i + 18, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
			else {
				menu.addItem(i + 18, glassPane.color(DyeColor.GRAY).display(QuestWorld.getInstance().getBookLocal("quests.display.rewards")).get());
				menu.addMenuClickHandler(i + 18, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						return false;
					}
				});
			}
		}
		
		int slot = 27;
		for (ItemStack reward: quest.getRewards()) {
			menu.addItem(slot, reward);
			menu.addMenuClickHandler(slot, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					return false;
				}
			});
			slot++;
		}
		
		menu.open(p);
	}

	
	/*
	 * 
	 * 			Quest Editor
	 * 
	 */
	
	
	@SuppressWarnings("deprecation")
	public static void openEditor(Player p) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		for (int i = 0; i < 45; i++) {
			final Category category = QuestWorld.getInstance().getCategory(i);
			List<String> lore = new ArrayList<String>();
			if (category != null) {
				ItemStack item = category.getItem();
				lore.add("");
				lore.add("§c§oLeft Click to edit");
				lore.add("§c§oShift + Left Click to open");
				lore.add("§c§oRight Click to delete");
				ItemMeta im = item.getItemMeta();
				im.setLore(lore);
				item.setItemMeta(im);
				menu.addItem(i, item);
				menu.addMenuClickHandler(i, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						if (!action.isRightClicked() && action.isShiftClicked()) openCategoryQuestEditor(p, category);
						else if (!action.isRightClicked() && !action.isShiftClicked()) {
							openCategoryEditor(p, category);
						}
						else if (action.isRightClicked()) {
							QBDialogue.openDeletionConfirmation(p, category);
						}
						return false;
					}
				});
			}
			else {
				ItemStack item = new CustomItem(new MaterialData(Material.STAINED_GLASS_PANE, (byte) 14), "&7&o> New Category");
				ItemMeta im = item.getItemMeta();
				im.setLore(lore);
				item.setItemMeta(im);
				menu.addItem(i, item);
				menu.addMenuClickHandler(i, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.create-category", true);
						QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.CATEGORY_CREATION, slot));
						p.closeInventory();
						return false;
					}
				});
			}
		}
		menu.build().open(p);
	}

	@SuppressWarnings("deprecation")
	public static void openCategoryQuestEditor(Player p, final Category category) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c< Back"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openEditor(p);
				return false;
			}
		});
		
		for (int i = 0; i < 45; i++) {
			final Quest quest = category.getQuest(i);
			List<String> lore = new ArrayList<String>();
			if (quest != null) {
				ItemStack item = quest.getItem();
				lore.add("");
				lore.add("§c§oLeft Click to edit");
				lore.add("§c§oRight Click to delete");
				ItemMeta im = item.getItemMeta();
				im.setLore(lore);
				item.setItemMeta(im);
				menu.addItem(i + 9, item);
				menu.addMenuClickHandler(i + 9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						if (!action.isRightClicked()) openQuestEditor(p, quest);
						else QBDialogue.openDeletionConfirmation(p, quest);
						return false;
					}
				});
			}
			else {
				ItemStack item = new CustomItem(new MaterialData(Material.STAINED_GLASS_PANE, (byte) 14), "&7&o> New Quest");
				ItemMeta im = item.getItemMeta();
				im.setLore(lore);
				item.setItemMeta(im);
				menu.addItem(i + 9, item);
				menu.addMenuClickHandler(i + 9, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.create-quest", true);
						QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.QUEST_CREATION, String.valueOf(category.getID()) + " M " + String.valueOf(slot - 9)));
						p.closeInventory();
						return false;
					}
				});
			}
		}
		menu.build().open(p);
	}

	@SuppressWarnings("deprecation")
	public static void openCategoryEditor(Player p, final Category category) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c< Back"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openEditor(p);
				return false;
			}
		});
		
		ItemStack item = category.getItem();
		ItemMeta im = item.getItemMeta();
		im.setLore(Arrays.asList("", "§e> Click to change the Item to", "§ethe Item you are currently holding"));
		item.setItemMeta(im);
		
		menu.addItem(9, item);
		menu.addMenuClickHandler(9, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (p.getItemInHand() != null && p.getItemInHand().getType() != null && p.getItemInHand().getType() != Material.AIR) {
					category.setItem(p.getItemInHand());
					openCategoryEditor(p, category);
				}
				return false;
			}
		});
		
		menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), category.getName(), "", "§e> Click to change the Name"));
		menu.addMenuClickHandler(10, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.CATEGORY_RENAME, category));
				QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.rename-category", true);
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(11, new CustomItem(new MaterialData(Material.BOOK_AND_QUILL), "§7Quest Requirement:", "", (category.getParent() != null ? "§r" + category.getParent().getName(): "§7§oNone"), "", "§rLeft Click: §eChange Quest Requirement", "§rRight Click: §eRemove Quest Requirement"));
		menu.addMenuClickHandler(11, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (action.isRightClicked()) {
					category.setParent(null);
					openCategoryEditor(p, category);
				}
				else QBDialogue.openQuestRequirementChooser(p, category);
				return false;
			}
		});
		
		menu.addItem(12, new CustomItem(new MaterialData(Material.NAME_TAG), "§r" + (category.getPermission().equals("") ? "None": category.getPermission()), "", "§e> Click to change the rquired Permission Node"));
		menu.addMenuClickHandler(12, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.CATEGORY_PERMISSION, category));
				QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.permission-category", true);
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(13, new CustomItem(new MaterialData(Material.GOLDEN_CARROT), "§rShow in Quest Book: " + (!category.isHidden() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Click to change whether this Category", "&ewill appear in the Quest Book"));
		menu.addMenuClickHandler(13, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				category.setHidden(!category.isHidden());
				openCategoryEditor(p, category);
				return false;
			}
		});
		
		menu.addItem(14, new CustomItem(new MaterialData(Material.GRASS), "§7World Blacklist", "", "§e> Click to configure in which Worlds", "&ethis Category is enabled"));
		menu.addMenuClickHandler(14, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openWorldEditor(p, category);
				return false;
			}
		});
		
		menu.addItem(17, new CustomItem(new MaterialData(Material.WOOL, (byte) 14), "§4Delete Database", "", "§rThis is going to delete the Database", "§rof all Quests inside this Category", "§rand will clear all Player's Progress associated", "§rwith those Quests."));
		menu.addMenuClickHandler(17, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				for (Quest quest: category.getQuests()) {
					QuestManager.clearAllQuestData(quest);
				}
				QuestWorld.getSounds().DestructiveClick().playTo(p);
				return false;
			}
		});
		
		menu.open(p);
	}

	@SuppressWarnings("deprecation")
	public static void openQuestEditor(Player p, final Quest quest) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c< Back"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openCategoryQuestEditor(p, quest.getCategory());
				return false;
			}
		});
		
		ItemStack item = quest.getItem();
		ItemMeta im = item.getItemMeta();
		im.setLore(Arrays.asList("", "§e> Click to change the Item to", "§ethe Item you are currently holding"));
		item.setItemMeta(im);
		
		menu.addItem(9, item);
		menu.addMenuClickHandler(9, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (p.getItemInHand() != null && p.getItemInHand().getType() != null && p.getItemInHand().getType() != Material.AIR) {
					quest.setItem(p.getItemInHand());
					openQuestEditor(p, quest);
				}
				return false;
			}
		});
		
		menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), quest.getName(), "", "§e> Click to change the Name"));
		menu.addMenuClickHandler(10, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.QUEST_RENAME, quest));
				QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.rename-quest", true);
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(11, new CustomItem(new MaterialData(Material.CHEST), "§rRewards §7(Item)", "", "§e> Click to change the Rewards", "§eto be the Items in your Hotbar"));
		menu.addMenuClickHandler(11, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				quest.setItemRewards(p);
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(12, new CustomItem(new MaterialData(Material.WATCH), "§7Cooldown: §b" + quest.getFormattedCooldown(), "", "§rLeft Click: §e+1m", "§rRight Click: §e-1m", "§rShift + Left Click: §e+1h", "§rShift + Right Click: §e-1h"));
		menu.addMenuClickHandler(12, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				long cooldown = quest.getCooldown() / 60 / 1000;
				if (action.isRightClicked()) cooldown = cooldown - (action.isShiftClicked() ? 60: 1);
				else cooldown = cooldown + (action.isShiftClicked() ? 60: 1);
				if (cooldown < 0) cooldown = 0;
				quest.setCooldown(cooldown);
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		if (QuestWorld.getInstance().getEconomy() != null) {
			menu.addItem(13, new CustomItem(new MaterialData(Material.GOLD_INGOT), "§7Monetary Reward: §6$" + quest.getMoney(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+100", "§rShift + Right Click: §e-100"));
			menu.addMenuClickHandler(13, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int money = quest.getMoney();
					if (action.isRightClicked()) money = money - (action.isShiftClicked() ? 100: 1);
					else money = money + (action.isShiftClicked() ? 100: 1);
					if (money < 0) money = 0;
					quest.setMoney(money);
					openQuestEditor(p, quest);
					return false;
				}
			});
		}
		
		menu.addItem(14, new CustomItem(new MaterialData(Material.EXP_BOTTLE), "§7XP Reward: §b" + quest.getXP() + " Level", "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+10", "§rShift + Right Click: §e-10"));
		menu.addMenuClickHandler(14, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				int xp = quest.getXP();
				if (action.isRightClicked()) xp = xp - (action.isShiftClicked() ? 10: 1);
				else xp = xp + (action.isShiftClicked() ? 10: 1);
				if (xp < 0) xp = 0;
				quest.setXP(xp);
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(15, new CustomItem(new MaterialData(Material.BOOK_AND_QUILL), "§7Quest Requirement:", "", (quest.getParent() != null ? "§r" + quest.getParent().getName(): "§7§oNone"), "", "§rLeft Click: §eChange Quest Requirement", "§rRight Click: §eRemove Quest Requirement"));
		menu.addMenuClickHandler(15, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (action.isRightClicked()) {
					quest.setParent(null);
					openQuestEditor(p, quest);
				}
				else QBDialogue.openQuestRequirementChooser(p, quest);
				return false;
			}
		});
		
		menu.addItem(16, new CustomItem(new MaterialData(Material.COMMAND), "§7Commands executed upon Completion", "", "§rLeft Click: §eOpen Command Editor"));
		menu.addMenuClickHandler(16, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				p.closeInventory();
				QBDialogue.openCommandEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(17, new CustomItem(new MaterialData(Material.NAME_TAG), "§r" + (quest.getPermission().equals("") ? "None": quest.getPermission()), "", "§e> Click to change the required Permission Node"));
		menu.addMenuClickHandler(17, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.QUEST_PERMISSION, quest));
				QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.permission-quest", true);
				p.closeInventory();
				return false;
			}
		});
		
		menu.addItem(18, new CustomItem(new MaterialData(Material.FIREWORK), "§rParty Support: " + (quest.supportsParties() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Click to change whether this Quest can be done in Parties or not"));
		menu.addMenuClickHandler(18, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				quest.setPartySupport(quest.supportsParties());
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(19, new CustomItem(new MaterialData(Material.COMMAND), "§rOrdered Completion Mode: " + (quest.isOrdered() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Click to change whether this Quest's Tasks", "§ehave to be done in the Order they are arranged"));
		menu.addMenuClickHandler(19, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				quest.setOrdered(!quest.isOrdered());
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(20, new CustomItem(new MaterialData(Material.CHEST), "§rAuto-Claim Rewards: " + (quest.isAutoClaiming() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Click to change whether this Quest's Rewards", "§ewill be automatically given or have to be", "§eclaimed manually"));
		menu.addMenuClickHandler(20, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				quest.setAutoClaim(!quest.isAutoClaiming());
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(21, new CustomItem(new MaterialData(Material.GRASS), "§7World Blacklist", "", "§e> Click to configure in which Worlds", "&ethis Quest is able to be completed"));
		menu.addMenuClickHandler(21, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openWorldEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(22, new CustomItem(new MaterialData(Material.FIREWORK), "§rMinimal Party Size: " + (quest.getPartySize() < 1 ? "§4Players aren't allowed be in a Party": (quest.getPartySize() == 1 ? ("§ePlayers can but don't have to be in a Party") : ("§aPlayers need to be in a Party of " + quest.getPartySize() + " or more"))), "", "§eChange the min. Amount of Players in", "§ea Party needed to start this Quest", "", "§r1 = §7Players can but don't have to be in a Party", "§r0 = §7Players aren't allowed to be in a Party", "", "§rLeft Click: §e+1", "§rRight Click: §e-1"));
		menu.addMenuClickHandler(22, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				int size = quest.getPartySize();
				if (action.isRightClicked()) size--;
				else size++;
				if (size < 0) size = 0;
				quest.setPartySize(size);
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		menu.addItem(26, new CustomItem(new MaterialData(Material.WOOL, (byte) 14), "§4Delete Database", "", "§rThis is going to delete this Quest's Database", "§rand will clear all Player's Progress associated", "§rwith this Quest."));
		menu.addMenuClickHandler(26, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				QuestManager.clearAllQuestData(quest);
				QuestWorld.getSounds().DestructiveClick().playTo(p);
				return false;
			}
		});
		
		int index = 36;
		for (ItemStack reward: quest.getRewards()) {
			menu.addItem(index, reward);
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player arg0, int arg1, ItemStack arg2, ClickAction arg3) {
					return false;
				}
			});
			index++;
		}
		
		for (int i = 0; i < 9; i++) {
			final QuestMission mission = quest.getMission(i);
			if (mission == null) {
				menu.addItem(45 + i, new CustomItem(new MaterialData(Material.PAPER), "&7&o> New Task"));
				menu.addMenuClickHandler(45 + i, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						quest.addMission(new QuestMission(quest, String.valueOf(slot - 36), MissionType.valueOf("SUBMIT"), EntityType.PLAYER, "", new ItemStack(Material.STONE), p.getLocation().getBlock().getLocation(), 1, null, 0, false, 0, false, "Hey there! Do this Quest.", null));
						openQuestEditor(p, quest);
						return false;
					}
				});
			}
			else {
				ItemStack stack = new CustomItem(new MaterialData(Material.BOOK), mission.getText());
				ItemMeta meta = stack.getItemMeta();
				meta.setLore(Arrays.asList("", "§c§oLeft Click to edit", "§c§oRight Click to delete"));
				stack.setItemMeta(meta);
				menu.addItem(45 + i, stack);
				menu.addMenuClickHandler(45 + i, new MenuClickHandler() {
					
					@Override
					public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
						if (action.isRightClicked()) QBDialogue.openDeletionConfirmation(p, mission);
						else openQuestMissionEditor(p, mission);
						return false;
					}
				});
			}
		}
		
		menu.open(p);
	}

	public static void openWorldEditor(Player p, final Quest quest) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c< Back"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openQuestEditor(p, quest);
				return false;
			}
		});
		
		int index = 9;
		for (final World world: Bukkit.getWorlds()) {
			menu.addItem(index, new CustomItem(new MaterialData(Material.GRASS), "&r" + world.getName() + ": " + (quest.isWorldEnabled(world.getName()) ? "§2§l\u2714": "§4§l\u2718")));
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					quest.toggleWorld(world.getName());
					openWorldEditor(p, quest);
					return false;
				}
			});
			index++;
		}
		
		menu.open(p);
	}

	public static void openWorldEditor(Player p, final Category category) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c< Back"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openCategoryEditor(p, category);
				return false;
			}
		});
		
		int index = 9;
		for (final World world: Bukkit.getWorlds()) {
			menu.addItem(index, new CustomItem(new MaterialData(Material.GRASS), "&r" + world.getName() + ": " + (category.isWorldEnabled(world.getName()) ? "§2§l\u2714": "§4§l\u2718")));
			menu.addMenuClickHandler(index, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					category.toggleWorld(world.getName());
					openWorldEditor(p, category);
					return false;
				}
			});
			index++;
		}
		
		menu.open(p);
	}

	@SuppressWarnings("deprecation")
	public static void openQuestMissionEditor(Player p, final QuestMission mission) {
		final ChestMenu menu = new ChestMenu("§3Quest Editor");
		menu.addMenuOpeningHandler(new MenuOpeningHandler() {
			
			@Override
			public void onOpen(Player p) {
				QuestWorld.getSounds().EditorClick().playTo(p);
			}
		});
		
		menu.addItem(0, new CustomItem(new MaterialData(Material.MAP), "&c< Back"));
		menu.addMenuClickHandler(0, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				openQuestEditor(p, mission.getQuest());
				return false;
			}
		});
		
		switch (mission.getType().getSubmissionType()) {
		
		case ENTITY: {
			EntityType entity = mission.getEntity();
			ItemBuilder egg = new ItemBuilder(Material.MONSTER_EGG).mob(entity);
			egg.display("&7Entity Type: &r" + Text.niceName(entity.name()));
			egg.lore("", "&e> Click to change the Entity");

			menu.addItem(10, egg.get());
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QBDialogue.openQuestMissionEntityEditor(p, mission);
					return false;
				}
			});
			
			menu.addItem(11, new CustomItem(new MaterialData(Material.MOB_SPAWNER), "§7Allow Mobs from Spawners: " + (mission.acceptsSpawners() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Click to change whether this Mission will", "§ealso count Mobs which were spawned by a Mob Spawner"));
			menu.addMenuClickHandler(11, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					mission.setSpawnerSupport(mission.acceptsSpawners());
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			
			menu.addItem(17, new CustomItem(new MaterialData(Material.REDSTONE), "§7Amount: §b" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case ITEM: {
			ItemStack item = mission.getItem();
			ItemMeta im = item.getItemMeta();
			im.setLore(Arrays.asList("", "§e> Click to change the Item to", "§ethe Item you are currently holding"));
			item.setItemMeta(im);
			
			menu.addItem(10, item);
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					if (p.getItemInHand() != null && p.getItemInHand().getType() != null && p.getItemInHand().getType() != Material.AIR) {
						mission.setItem(p.getItemInHand());
						openQuestMissionEditor(p, mission);
					}
					return false;
				}
			});
			
			menu.addItem(17, new CustomItem(new MaterialData(Material.REDSTONE), "§7Amount: §b" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case BLOCK: {
			ItemStack item = mission.getItem();
			ItemMeta im = item.getItemMeta();
			im.setLore(Arrays.asList("", "§e> Click to change the Block to", "§ethe Item you are currently holding"));
			item.setItemMeta(im);
			
			menu.addItem(10, item);
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					if (p.getItemInHand() != null && p.getItemInHand().getType() != null && p.getItemInHand().getType() != Material.AIR && p.getItemInHand().getType().isBlock()) {
						mission.setItem(new ItemStack(p.getItemInHand().getType()));
						openQuestMissionEditor(p, mission);
					}
					return false;
				}
			});
			
			menu.addItem(17, new CustomItem(new MaterialData(Material.REDSTONE), "§7Amount: §b" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case INTEGER: {
			menu.addItem(17, new CustomItem(new MaterialData(Material.REDSTONE), "§7Amount: §b" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case TIME: {
			menu.addItem(17, new CustomItem(new MaterialData(Material.WATCH), "§7Time: §b" + (mission.getAmount() / 60) + "h " + (mission.getAmount() % 60) + "m", "", "§rLeft Click: §e+1m", "§rRight Click: §e-1m", "§rShift + Left Click: §e+1h", "§rShift + Right Click: §e-1h"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 60: 1);
					else amount = amount + (action.isShiftClicked() ? 60: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case LOCATION: {
			ItemStack item = mission.getItem();
			ItemMeta im = item.getItemMeta();
			im.setLore(Arrays.asList("", "§e> Click to change the Location", "§eto your current Position"));
			item.setItemMeta(im);
			
			menu.addItem(10, item);
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					mission.setLocation(p);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			
			menu.addItem(11, new CustomItem(new MaterialData(Material.NAME_TAG), "§r" + mission.getEntityName(), "", "§e> Give your Location a Name"));
			menu.addMenuClickHandler(11, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.LOCATION_NAME, mission));
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.rename-location", true);
					p.closeInventory();
					return false;
				}
			});
			
			menu.addItem(17, new CustomItem(new MaterialData(Material.COMPASS), "§7Radius: §a" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case CITIZENS_INTERACT: {
			menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), "§dCitizen §f#" + mission.getCitizenID(), "§7Name: §r" + (mission.getCitizen() != null ? mission.getCitizen().getName(): "§4N/A"), "", "§e> Click to change the selected NPC"));
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.link-citizen", true);
					CitizensListener.link.put(p.getUniqueId(), mission);
					p.closeInventory();
					return false;
				}
			});
			break;
		}
		
		case CITIZENS_KILL: {
			menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), "§dCitizen §f#" + mission.getCitizenID(), "§7Name: §r" + (mission.getCitizen() != null ? mission.getCitizen().getName(): "§4N/A"), "", "§e> Click to change the selected NPC"));
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.link-citizen", true);
					CitizensListener.link.put(p.getUniqueId(), mission);
					p.closeInventory();
					return false;
				}
			});
			
			menu.addItem(17, new CustomItem(new MaterialData(Material.REDSTONE), "§7Amount: §b" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}
		
		case CITIZENS_ITEM: {
			menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), "§dCitizen §f#" + mission.getCitizenID(), "§7Name: §r" + (mission.getCitizen() != null ? mission.getCitizen().getName(): "§4N/A"), "", "§e> Click to change the selected NPC"));
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.link-citizen", true);
					CitizensListener.link.put(p.getUniqueId(), mission);
					p.closeInventory();
					return false;
				}
			});
			
			ItemStack item = mission.getItem();
			ItemMeta im = item.getItemMeta();
			im.setLore(Arrays.asList("", "§e> Click to change the Item to", "§ethe Item you are currently holding"));
			item.setItemMeta(im);
			
			menu.addItem(11, item);
			menu.addMenuClickHandler(11, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					if (p.getItemInHand() != null && p.getItemInHand().getType() != null && p.getItemInHand().getType() != Material.AIR) {
						mission.setItem(p.getItemInHand());
						openQuestMissionEditor(p, mission);
					}
					return false;
				}
			});
			
			menu.addItem(17, new CustomItem(new MaterialData(Material.REDSTONE), "§7Amount: §b" + mission.getAmount(), "", "§rLeft Click: §e+1", "§rRight Click: §e-1", "§rShift + Left Click: §e+16", "§rShift + Right Click: §e-16"));
			menu.addMenuClickHandler(17, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					int amount = mission.getAmount();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
					else amount = amount + (action.isShiftClicked() ? 16: 1);
					if (amount < 1) amount = 1;
					mission.setAmount(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
			break;
		}

		case COMMAND: {
			menu.addItem(10, new CustomItem(new MaterialData(Material.NAME_TAG), "§dCommand", mission.getCommand() == null ? "" : mission.getCommand(), "§rLeft Click to set"));
			menu.addMenuClickHandler(10, new MenuClickHandler() {
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					p.closeInventory();
					p.sendMessage("§7Enter the command in chat");
					QuestWorld.getInstance().addChatCallback(p, new ChatInputCallback() {
						@Override
						public void run(String command) {
							mission.setCommand(command);
							openQuestMissionEditor(p, mission);
						}
					});
					return false;
				}
			});
		}
		
		default:
			break;
		}
		
		if (mission.getType().getID().equals("KILL_NAMED_MOB")) {
			menu.addItem(12, new CustomItem(new MaterialData(Material.NAME_TAG), "§r" + mission.getEntityName(), "", "§e> Click to change the Name"));
			menu.addMenuClickHandler(12, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestWorld.getInstance().storeInput(p.getUniqueId(), new Input(InputType.KILL_NAMED, mission));
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.rename-kill-mission", true);
					p.closeInventory();
					return false;
				}
			});
		}
		
		if (mission.getType().getID().equals("ACCEPT_QUEST_FROM_NPC")) {
			List<String> lore = new ArrayList<String>();
			lore.add("");
			for (String s: new String("§r" + mission.getLore()).replaceAll(".{32}", "$0NEW LINE§r").split("NEW LINE")) {
				lore.add(s);
			}
			lore.add("");
			lore.add("§e> Edit the Quest's Description");
			lore.add("&7(Color Codes are not supported)");
			
			menu.addItem(11, new CustomItem(new MaterialData(Material.NAME_TAG), "§rQuest Description", lore.toArray(new String[lore.size()])));
			menu.addMenuClickHandler(11, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.misssion-description", true);
					MenuHelper.awaitChatInput(p, new ChatHandler() {
						
						@Override
						public boolean onChat(Player p, String message) {
							mission.setLore(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
							openQuestMissionEditor(p, mission);
							return false;
						}
					});
					p.closeInventory();
					return false;
				}
			});
		}
		
		int totalMissions = QuestWorld.getInstance().getMissionTypes().size();
		String[] missionTypes = new String[totalMissions];
		
		int i = 0;
		int missionIndex = -1;
		
		final String[] keys = QuestWorld.getInstance().getMissionTypes().keySet().toArray(new String[totalMissions]);
		
		for (String type: keys) {
			if(type.equals(mission.getType().toString()))
				missionIndex = i;
			missionTypes[i++] = Text.niceName(type);
		}
		
		ItemStack missionSelector = new ItemBuilder(mission.getType().getItem().toItemStack(1))
				.display("&7" + missionTypes[missionIndex])
				.selector(missionIndex, missionTypes)
				.get();
		
		final int currentMission = missionIndex;
		menu.addItem(9, missionSelector);
		menu.addMenuClickHandler(9, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				int delta = 1;
				if(action.isRightClicked())
					delta = -1;
				
				int newMission = (currentMission + delta + totalMissions) % totalMissions;
				
				mission.setType(QuestWorld.getInstance().getMissionTypes().get(keys[newMission]));
				
				//mission.setType(mission.getType().getNextType());
				QuestManager.updateTickingTasks();
				openQuestMissionEditor(p, mission);
				return false;
			}
		});
		
		if (mission.getType().supportsDeathReset()) {
			menu.addItem(5, new CustomItem(new MaterialData(Material.SKULL_ITEM), "§7Resets on Death: " + (mission.resetsonDeath() ? "§2§l\u2714": "§4§l\u2718"), "", "§e> Click to change whether this Mission's Progress", "§eresets when a Player dies"));
			menu.addMenuClickHandler(5, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					mission.setDeathReset(!mission.resetsonDeath());
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
		}
		
		if (mission.getType().supportsTimeframes()) {
			menu.addItem(6, new CustomItem(new MaterialData(Material.WATCH), "§7Complete Mission within: §b" + (mission.getTimeframe() / 60) + "h " + (mission.getTimeframe() % 60) + "m", "", "§rLeft Click: §e+1m", "§rRight Click: §e-1m", "§rShift + Left Click: §e+1h", "§rShift + Right Click: §e-1h"));
			menu.addMenuClickHandler(6, new MenuClickHandler() {
				
				@Override
				public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
					long amount = mission.getTimeframe();
					if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 60: 1);
					else amount = amount + (action.isShiftClicked() ? 60: 1);
					if (amount < 0) amount = 0;
					mission.setTimeframe(amount);
					openQuestMissionEditor(p, mission);
					return false;
				}
			});
		}
		
		menu.addItem(7, new CustomItem(new MaterialData(Material.NAME_TAG), "§rCustom Name", mission.getText(), "", "§rLeft Click: Edit Mission Name", "§rRight Click: Reset Mission Name"));
		menu.addMenuClickHandler(7, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (action.isRightClicked()) {
					mission.setCustomName(null);
					openQuestMissionEditor(p, mission);
				}
				else {
					p.closeInventory();
					QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.await-mission-name", true);
					MenuHelper.awaitChatInput(p, new ChatHandler() {
						
						@Override
						public boolean onChat(Player p, String message) {
							mission.setCustomName(message);
							QuestWorld.getInstance().getLocalization().sendTranslation(p, "editor.edit-mission-name", true);
							openQuestMissionEditor(p, mission);
							return false;
						}
					});
				}
				return false;
			}
		});
		
		menu.addItem(8, new CustomItem(new MaterialData(Material.PAPER), "§rDialogue", "", "§rLeft Click: Edit the Dialogue", "§rRight Click: Dialogue Preview"));
		menu.addMenuClickHandler(8, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				if (action.isRightClicked()) {
					p.closeInventory();
					if (mission.getDialogue().isEmpty()) p.sendMessage("§4No Dialogue found!");
					else QuestWorld.getInstance().getManager(p).sendQuestDialogue(p, mission, mission.getDialogue().iterator());
				}
				else {
					p.closeInventory();
					mission.setupDialogue(p);
				}
				return false;
			}
		});
		
		menu.open(p);
	}

}
