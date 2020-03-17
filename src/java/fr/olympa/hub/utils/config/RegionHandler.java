package fr.olympa.hub.utils.config;

import org.bukkit.Location;

public class RegionHandler {
	private ConfigManager fb;
	private String region;

	public RegionHandler(String region) {
		this.fb = new ConfigManager("plugins//OlympaHub//Region/", region + ".yml");
		this.fb.save();
		this.region = region;
	}

	public String getRegion() {
		return region;
	}

	public boolean exist() {
		return this.fb.exist();
	}

	public void remove() {
		this.fb.removeFile(region + ".yml");
	}

	public void setRegion(Location loc1, Location loc2) {
		String coords1 = "X: " + loc1.getBlockX() + " Y: " + loc1.getBlockY() + " Z: " + loc1.getBlockZ();
		String coords2 = "X: " + loc2.getBlockX() + " Y: " + loc2.getBlockY() + " Z: " + loc2.getBlockZ();
		this.fb.setValue("region.coords1", coords1);
		this.fb.setValue("region.coords2", coords2);
		this.fb.setValue("region.loc1.x", loc1.getX());
		this.fb.setValue("region.loc1.y", loc1.getY());
		this.fb.setValue("region.loc1.z", loc1.getZ());

		this.fb.setValue("region.loc2.x", loc2.getX());
		this.fb.setValue("region.loc2.y", loc2.getY());
		this.fb.setValue("region.loc2.z", loc2.getZ());

		this.fb.save();
	}

	public void removeRegion() {
		fb.setValue("region", null);
		this.fb.save();
	}

	public int getLoc1X() {
		return this.fb.getInt("region.loc1.x");
	}

	public int getLoc1Y() {
		return this.fb.getInt("region.loc1.y");
	}

	public int getLoc1Z() {
		return this.fb.getInt("region.loc1.z");
	}

	public int getLoc2X() {
		return this.fb.getInt("region.loc2.x");
	}

	public int getLoc2Y() {
		return this.fb.getInt("region.loc2.y");
	}

	public int getLoc2Z() {
		return this.fb.getInt("region.loc2.z");
	}

	public String getCoords1() {
		return this.fb.getString("region.coords1");
	}

	public String getCoords2() {
		return this.fb.getString("region.coords1");
	}

	public void setLoc1(Location loc1) {
		this.fb.setValue("faction.loc1", loc1);
		this.fb.save();
	}

	public void setLoc2(Location loc2) {
		this.fb.setValue("faction.loc2", loc2);
		this.fb.save();
	}

}
