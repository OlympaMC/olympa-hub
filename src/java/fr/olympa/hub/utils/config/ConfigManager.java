package fr.olympa.hub.utils.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
	private File f;
	private YamlConfiguration c;
	private String name;
	private String path;

	public ConfigManager(String FilePath, String FileName) {
		this.f = new File(FilePath, FileName);
		this.path = FilePath;
		this.name = FileName;
		this.c = YamlConfiguration.loadConfiguration(this.f);
	}

	public ConfigManager setValue(String ValuePath, Object Value) {
		this.c.set(ValuePath, Value);
		return this;
	}

	public void setName(String name) {
		File newFile = new File(path, name + ".yml");
		this.f.renameTo(newFile);
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public Object getObject(String ValuePath) { return this.c.get(ValuePath); }

	public void removeFile(String FileName) {
		if (f.getAbsoluteFile().getName() != null) {
			f.getAbsoluteFile().delete();
		}else {

		}
	}

	public void removeBuilder(String FilePath) { this.f.delete(); }

	public int getInt(String ValuePath) { return this.c.getInt(ValuePath); }

	public String getString(String ValuePath) { return this.c.getString(ValuePath); }
//t
	public long getLong(String ValuePath) { return this.c.getLong(ValuePath); }

	public boolean getBoolean(String ValuePath) { return this.c.getBoolean(ValuePath); }

	public List<String> getStringList(String ValuePath) { return this.c.getStringList(ValuePath); }



	public Set<String> getKeys(boolean dep) { return this.c.getKeys(dep); }



	public ConfigurationSection getConfigurationSection(String section) { return this.c.getConfigurationSection(section); }



	public boolean exist() { return this.f.exists(); }


	public ConfigManager save() {
		try {
			this.c.save(this.f);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return this;
	}
}
