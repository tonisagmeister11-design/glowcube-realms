import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedural texture generator for Glowcube's Realms.
 * Run from the project root: java tools/TextureGen.java
 */
public class TextureGen {
	static final String ROOT = "src/main/resources/assets/glowcube_realms/textures/";
	static final String MC_ROOT = "src/main/resources/assets/minecraft/textures/";

	// ---------------------------------------------------------------- palettes (dark -> light)
	static final int[] SKYSTONE = {0x5d6b85, 0x75849e, 0x8d9cb5, 0xa5b4cb, 0xbfcde0, 0xdbe6f2};
	static final int[] LUMEN_SOIL = {0x3d2f4f, 0x4f3d63, 0x624c78, 0x77608c, 0x8c76a0};
	static final int[] LUMEN_GRASS = {0x137a7a, 0x1d9c93, 0x2bbfa9, 0x46dcc0, 0x7ff5da, 0xc9fff0};
	static final int[] GLOWCRYSTAL = {0x0d4f7a, 0x1b7fb5, 0x2fb4e6, 0x6fdcff, 0xb5f2ff, 0xffffff};
	static final int[] AURORA_BARK = {0x2b1d3d, 0x3c2955, 0x4f376d, 0x664a87, 0x7f60a1};
	static final int[] AURORA_WOOD = {0x8a5aa8, 0xa06fbf, 0xb887d4, 0xcfa2e6, 0xe4c2f5};
	static final int[] AURORA_LEAF = {0x6b2a8f, 0x8f3bb3, 0xb558d4, 0xd681ea, 0xf0b5ff, 0xfff0ff};
	static final int[] UMBRAL_STONE = {0x120d1a, 0x1b1426, 0x251c33, 0x312641, 0x3e3151, 0x4d3f63};
	static final int[] UMBRAL_MOSS = {0x0b2a2e, 0x0f3b3d, 0x15504f, 0x1d6862, 0x27857a};
	static final int[] VOIDSHARD = {0x2a0a4a, 0x4a1580, 0x7225b8, 0x9b4ae0, 0xc88bff, 0xf1dcff};
	static final int[] SHADECAP = {0x1f0b33, 0x2e1249, 0x421c63, 0x58287e, 0x70369a};
	static final int[] GLOW_SPOT = {0x2fd6c4, 0x5cf2dd, 0xa8fff1, 0xffffff};
	static final int[] WOOD = {0x3b2614, 0x553a1f, 0x6f4e2b, 0x8a663a, 0xa6804c};
	static final int[] GOLD = {0x6b4100, 0x9c6500, 0xd19a0f, 0xf2c53d, 0xffe58a, 0xfffbe0};
	static final int[] RADIANT = {0x8a6a12, 0xd1a21f, 0xffe066, 0xfff4b8, 0xffffff, 0xffffff};
	static final int[] STARMETAL = {0x1c2340, 0x2f3a66, 0x47598f, 0x6b80b8, 0x9fb3e0, 0xe0e9ff};
	static final int[] SHADOW_STEEL = {0x0d0b12, 0x1c1826, 0x2d273b, 0x433a57, 0x5f5378, 0x8b7fa8};
	static final int[] BONE = {0x6e6450, 0x8f8468, 0xb0a585, 0xcfc6a6, 0xebe4cc};
	static final int[] EMBER = {0x4a0f05, 0x8a1f08, 0xc9400f, 0xf27a1d, 0xffb84d, 0xfff0b0};

	static Random rng = new Random(1);

	public static void main(String[] args) throws IOException {
		blocks();
		items();
		items2();
		items3();
		equipment();
		gui();
		EntityTex.all();
		System.out.println("Textures written.");
	}

	// ================================================================ ENTITIES
	/** Material for painting model cubes. */
	record Mat(int[] pal, String style, int[] glow, double glowChance) {
		Mat(int[] pal, String style) {
			this(pal, style, null, 0);
		}
	}

	interface Detail {
		/** Return an ARGB override for the base texture or 0; may paint glow pixels too. */
		int paint(BufferedImage glow, int x, int y, int face, int fx, int fy, int fw, int fh);
	}

	static class EntityTex {
		static BufferedImage img, glow;
		static double[][] noise;
		static long seed;

		static void begin(int w, int h, long s) {
			img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			glow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			noise = fbm(w, h, s, new int[]{8, 4, 2}, new double[]{0.45, 0.35, 0.2});
			seed = s;
		}

		static void end(String name) throws IOException {
			save(img, "entity/" + name);
			save(glow, "entity/" + name + "_glow");
		}

		static void cube(int u, int v, int w, int h, int d, Mat m, Detail detail) {
			Random r = new Random(seed + u * 31L + v * 17L);
			paintBox(img, u, v, w, h, d, (x, y, face, fx, fy, fw, fh) -> {
				double n = noise[x % noise.length][y % noise[0].length];
				int[] p = m.pal();
				int c;
				switch (m.style()) {
					case "crystal" -> {
						double band = ((fx + fy * 2 + face * 3) % 7) / 7.0;
						c = p[clamp((int) ((n * 0.6 + band * 0.4) * p.length), 0, p.length - 1)];
						if ((fx + fy) % 5 == 0) c = p[p.length - 1];
					}
					case "metal" -> {
						double grad = 1.0 - fy / (double) Math.max(1, fh);
						c = p[clamp((int) ((grad * 0.55 + n * 0.45) * p.length), 0, p.length - 1)];
						if (fy == 0 || fx == 0) c = p[p.length - 1];
						if (fy == fh - 1 || fx == fw - 1) c = p[0];
					}
					case "cloth" -> {
						c = p[clamp((int) (n * p.length), 0, p.length - 1)];
						if (fx % 4 == 0) c = shadeColor(c, 0.85);
					}
					case "fur" -> {
						double streak = ((x * 7 + (y / 2) * 3) % 5) / 5.0;
						c = p[clamp((int) ((n * 0.7 + streak * 0.3) * p.length), 0, p.length - 1)];
					}
					default -> {
						c = p[clamp((int) (n * p.length), 0, p.length - 1)];
						boolean edge = fx == 0 || fy == 0 || fx == fw - 1 || fy == fh - 1;
						if (edge && fw > 2 && fh > 2) c = shadeColor(c, 0.78);
					}
				}
				if (m.glow() != null && r.nextDouble() < m.glowChance()) {
					int g = m.glow()[r.nextInt(m.glow().length)];
					c = g;
					glow.setRGB(x, y, argb(g));
				}
				if (detail != null) {
					int o = detail.paint(glow, x, y, face, fx, fy, fw, fh);
					if (o != 0) c = o;
				}
				return c & 0xFFFFFF;
			});
		}

		static void cube(int u, int v, int w, int h, int d, Mat m) {
			cube(u, v, w, h, d, m, null);
		}

		/** Glows the whole cube (crystal parts). */
		static Detail glowAll(double chance) {
			return (g, x, y, face, fx, fy, fw, fh) -> {
				if (((x * 13 + y * 7) % 100) < chance * 100) g.setRGB(x, y, img.getRGB(x, y) == 0 ? argb(0xffffff) : img.getRGB(x, y) | 0xFF000000);
				return 0;
			};
		}

		static void glowPixel(int x, int y, int color) {
			img.setRGB(x, y, argb(color));
			glow.setRGB(x, y, argb(color));
		}

		static void copyGlowFromImage(int u, int v, int w, int h) {
			for (int y = v; y < v + h && y < img.getHeight(); y++) for (int x = u; x < u + w && x < img.getWidth(); x++) {
				int c = img.getRGB(x, y);
				if ((c >>> 24) != 0) glow.setRGB(x, y, c);
			}
		}

		static final int[] ICE = {0x4a7ab0, 0x6a9ad0, 0x8fbde8, 0xb8dcf8, 0xdff2ff, 0xffffff};
		static final int[] FROST_ROBE = {0x1a3a6a, 0x24508a, 0x3068a8, 0x4282c4, 0x5a9ade};
		static final int[] KING_BONE = {0x5a5440, 0x77705a, 0x948c72, 0xb2aa8e, 0xd0c8aa, 0xeee8d0};
		static final int[] SOUL = {0x0a3a3a, 0x146060, 0x1e8a8a, 0x3ab8b8, 0x7ae8e8, 0xd0ffff};
		static final int[] OBSIDIAN = {0x0c0814, 0x16101e, 0x22182e, 0x2e2040, 0x3a2a52};
		static final int[] MAGMA_ROCK = {0x1a1010, 0x2a1818, 0x3a2020, 0x4a2a24, 0x5a3428, 0x6a4030};

		static void all() throws IOException {
			wisp();
			golem("crystal_golem", 502, new int[]{0x3a3f4f, 0x4a5164, 0x5a627a, 0x6b7590, 0x7f8aa6}, new int[]{0x2fb4e6, 0x6fdcff, 0xb5f2ff}, CRYSTAL, 0x9ff2ff);
			golem("infernal_colossus", 512, MAGMA_ROCK, new int[]{0xf27a1d, 0xffb84d, 0xc9400f}, new Mat(EMBER, "crystal"), 0xffd060);
			crawler();
			guardianVillager("realm_guardian", 0);
			guardianVillager("realm_guardian_chain", 1);
			guardianVillager("realm_guardian_iron", 2);
			trader("realm_trader_lumen", new int[]{0x2a6a9a, 0x3a86b8, 0x56a6d4, 0x7ec4ec}, GLOWCRYSTAL, 0xf2c53d);
			trader("realm_trader_umbral", new int[]{0x2a1440, 0x3c1e5a, 0x522a78, 0x6a3a96}, VOIDSHARD, 0x9b4ae0);
			trader("realm_trader_sculk", new int[]{0x08262c, 0x0d3a42, 0x135058, 0x1a6a72}, new int[]{0x0d6a72, 0x29dfeb, 0x5cf2ff, 0xb0ffff, 0xffffff, 0xffffff}, 0x29dfeb);
			glowkeeper("glowkeeper", 505, new int[]{0x9c7a1f, 0xc9a23a, 0xe8c45c, 0xf8e08f, 0xfff6cf}, new int[]{0xb8c6d8, 0xd2dce8, 0xe8eef5, 0xffffff}, CRYSTAL,
					0x9ff2ff, 0x6fdcff);
			glowkeeper("void_herald", 515, new int[]{0x3a1a5a, 0x5a2a8a, 0x7a3aae, 0x9a5ad0, 0xc08aff}, OBSIDIAN, VOID_CRYSTAL, 0xff7aff, 0xd08bff);
			tyrant();
			warden("ember_warden", 507, new int[]{0x16121a, 0x221c28, 0x302838, 0x40364a, 0x584a64}, new Mat(EMBER, "crystal"),
					new int[]{0x4a0a0a, 0x6a1010, 0x8a1818, 0xa82020}, 0xffb84d, GOLD);
			warden("frost_lich", 517, FROST_ROBE, new Mat(ICE, "crystal"), new int[]{0x0e2440, 0x16345a, 0x204874, 0x2a5a8e}, 0x9ff8ff, ICE);
			warden("hollow_king", 518, KING_BONE, new Mat(SOUL, "crystal"), new int[]{0x12241a, 0x1a3424, 0x22442e, 0x2a5438}, 0x7ae8e8, GOLD);
			wyvern();
			creatures3();
			mapIcons();
			icon("boss_infernal_colossus", EMBER, MAGMA_ROCK);
			icon("boss_void_herald", VOIDSHARD, OBSIDIAN);
			icon("boss_frost_lich", ICE, FROST_ROBE);
			icon("boss_tempest_drake", STARMETAL, GLOWCRYSTAL);
			icon("boss_hollow_king", KING_BONE, SOUL);
		}

		static void wyvern() throws IOException {
			begin(128, 64, 519);
			Mat scales = new Mat(new int[]{0x1c2a40, 0x26385a, 0x304874, 0x3c5a8e, 0x4a6ea8}, "fur", new int[]{0xffe066, 0xfff4b8}, 0.03);
			Mat membrane = new Mat(new int[]{0x4a6a9a, 0x5a80b4, 0x6a96cc, 0x80aee0}, "cloth");
			int[] bolt = {0xffe066};
			cube(0, 0, 12, 10, 20, scales, (g, x, y, face, fx, fy, fw, fh) -> {
				if ((face == 2 || face == 4) && Math.abs(fy - (fh / 2.0 + Math.sin(fx * 0.8) * 2)) < 0.6) {
					g.setRGB(x, y, argb(bolt[0]));
					return bolt[0];
				}
				return face == 5 ? 0x8aa0c0 : 0;
			});
			cube(64, 0, 6, 6, 8, scales);
			cube(64, 14, 8, 7, 10, scales, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy == 2 && (fx == 1 || fx == 6)) {
					g.setRGB(x, y, argb(0xfff4b8));
					return 0xfff4b8;
				}
				if (face == 1 && fy >= 5 && fx % 2 == 0) return 0xe8e8e8;
				return 0;
			});
			cube(100, 0, 2, 6, 2, new Mat(BONE, "stone"));
			cube(0, 30, 24, 2, 16, membrane, (g, x, y, face, fx, fy, fw, fh) -> (face == 0 || face == 5) && fx % 6 == 0 ? 0x26385a : 0);
			cube(0, 48, 20, 1, 12, membrane, (g, x, y, face, fx, fy, fw, fh) -> (face == 0 || face == 5) && fx % 5 == 0 ? 0x26385a : 0);
			cube(100, 14, 4, 4, 10, scales);
			cube(86, 31, 3, 3, 8, new Mat(new int[]{0xffe066, 0xfff4b8}, "crystal"), glowAll(1.0));
			cube(108, 0, 3, 6, 3, scales);
			end("tempest_drake");
		}

		static final Mat CRYSTAL = new Mat(GLOWCRYSTAL, "crystal");
		static final Mat VOID_CRYSTAL = new Mat(VOIDSHARD, "crystal");

		static void wisp() throws IOException {
			begin(32, 32, 501);
			cube(0, 0, 6, 6, 6, new Mat(new int[]{0x8ae6ff, 0xb5f2ff, 0xd8fbff, 0xffffff}, "crystal"), (g, x, y, face, fx, fy, fw, fh) -> {
				g.setRGB(x, y, argb(0xc8f8ff));
				if (face == 1 && fy == 2 && (fx == 1 || fx == 4)) {
					g.setRGB(x, y, 0);
					return 0x0b2a3a;
				}
				if (face == 1 && fy == 4 && fx >= 2 && fx <= 3) return 0x2fb4e6;
				return 0;
			});
			cube(0, 12, 2, 2, 2, CRYSTAL, glowAll(1.0));
			cube(8, 12, 2, 4, 2, new Mat(new int[]{0x5cdfff, 0x8ae6ff, 0xc8f8ff}, "crystal"), glowAll(0.7));
			end("glow_wisp");
		}

		static void golem(String name, long seed, int[] rockPal, int[] glowPal, Mat crystal, int eye) throws IOException {
			begin(128, 64, seed);
			Mat rock = new Mat(rockPal, "stone", glowPal, 0.05);
			cube(0, 0, 14, 13, 8, rock);
			cube(0, 22, 8, 8, 8, rock, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy == 3 && (fx == 1 || fx == 2 || fx == 5 || fx == 6)) {
					g.setRGB(x, y, argb(eye));
					return eye;
				}
				if (face == 1 && fy == 2 && fx >= 1 && fx <= 6) return 0x2a2e3a;
				return 0;
			});
			cube(32, 22, 2, 5, 2, crystal, glowAll(0.8));
			cube(48, 0, 5, 18, 5, rock);
			cube(68, 0, 5, 18, 5, rock);
			cube(88, 0, 3, 7, 3, crystal, glowAll(0.8));
			cube(100, 0, 7, 6, 7, crystal, glowAll(0.5));
			cube(0, 40, 6, 11, 6, rock);
			cube(24, 40, 6, 11, 6, rock);
			end(name);
		}

		static void crawler() throws IOException {
			begin(64, 64, 503);
			Mat shell = new Mat(new int[]{0x0d0914, 0x181024, 0x241838, 0x30204a, 0x3e2a60}, "fur", new int[]{0x7225b8, 0x9b4ae0}, 0.04);
			cube(0, 0, 10, 6, 14, shell, (g, x, y, face, fx, fy, fw, fh) -> face == 0 && fx == fw / 2 ? 0x7225b8 : 0);
			cube(0, 20, 8, 6, 7, shell, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && (fy == 1 || fy == 3) && (fx == 1 || fx == 6 || (fy == 1 && (fx == 3 || fx == 4)))) {
					g.setRGB(x, y, argb(0xd08bff));
					return 0xd08bff;
				}
				return 0;
			});
			cube(30, 20, 2, 2, 3, new Mat(BONE, "stone"));
			cube(30, 26, 12, 2, 2, new Mat(new int[]{0x0d0914, 0x181024, 0x241838}, "stone"));
			cube(0, 34, 2, 4, 2, VOID_CRYSTAL, glowAll(0.8));
			end("shade_crawler");
		}

		static void guardian() throws IOException {
			begin(64, 64, 504);
			int[] skin = {0x9c6b4a, 0xb5815d, 0xc99572, 0xd9a886};
			int[] tabard = {0x1d6a8a, 0x2588b0, 0x2fa2d0, 0x48c0ea};
			int[] steel = {0x5b6370, 0x7a8494, 0x9aa5b5, 0xbcc6d4};
			cube(0, 0, 8, 8, 8, new Mat(skin, "stone"), (g, x, y, face, fx, fy, fw, fh) -> {
				if (fy <= 1) return 0x3b2614; // hair
				if (face == 1) {
					if (fy == 4 && (fx == 1 || fx == 6)) return 0xffffff;
					if (fy == 4 && (fx == 2 || fx == 5)) return 0x2a4a8a;
					if (fy == 3 && fx >= 1 && fx <= 6 && fx != 3 && fx != 4) return 0x3b2614;
					if (fy == 6 && fx >= 3 && fx <= 4) return 0x7a4a3a;
				}
				if (face != 1 && fy <= 3) return 0x3b2614;
				return 0;
			});
			cube(16, 16, 8, 12, 4, new Mat(tabard, "cloth"), (g, x, y, face, fx, fy, fw, fh) -> {
				if (fy >= 10) return 0x4a3220; // belt
				if (fy == 9) return 0xd19a0f;
				if (face == 1 || face == 3) {
					// Glowcube emblem: a glowing cube
					int cx = fx - 2, cy = fy - 2;
					if (cx >= 0 && cx < 4 && cy >= 0 && cy < 4) {
						boolean edge = cx == 0 || cy == 0 || cx == 3 || cy == 3;
						int col = edge ? 0x0b2a3a : (cx + cy) % 2 == 0 ? 0x9ff2ff : 0x5cdfff;
						if (!edge) g.setRGB(x, y, argb(col));
						return col;
					}
				}
				if (fx == 0 || fx == fw - 1) return 0xd19a0f;
				return 0;
			});
			cube(40, 16, 4, 12, 4, new Mat(steel, "metal"), (g, x, y, face, fx, fy, fw, fh) -> fy >= 10 ? 0x7a5a3a : 0);
			cube(0, 16, 4, 12, 4, new Mat(new int[]{0x2a2a3a, 0x34344a, 0x40405a}, "cloth"), (g, x, y, face, fx, fy, fw, fh) -> fy >= 9 ? 0x3b2614 : 0);
			save(img, "entity/realm_guardian");
		}

		static void glowkeeper(String name, long seed, int[] goldPal, int[] whitePal, Mat crystal, int eye, int core) throws IOException {
			begin(128, 128, seed);
			Mat gold = new Mat(goldPal, "metal");
			Mat white = new Mat(whitePal, "metal");
			cube(0, 0, 16, 14, 9, white, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1) {
					double d = Math.hypot(fx - 7.5, fy - 6);
					if (d < 2.2) {
						g.setRGB(x, y, argb(d < 1 ? 0xffffff : core));
						return d < 1 ? 0xffffff : core;
					}
					if (d < 3.2) return goldPal[2];
					if (Math.abs(fx - 7.5) + Math.abs(fy - 6) < 6 && (fx + fy) % 3 == 0) return goldPal[3];
				}
				if (fy == 0 || fy == fh - 1) return goldPal[2];
				return 0;
			});
			cube(0, 24, 12, 8, 7, gold);
			cube(40, 24, 7, 7, 4, crystal, glowAll(0.4));
			cube(62, 24, 3, 6, 3, crystal, glowAll(1.0));
			cube(0, 40, 10, 10, 10, white, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1) {
					if (fy >= 4 && fy <= 5 && (fx >= 1 && fx <= 3 || fx >= 6 && fx <= 8)) {
						g.setRGB(x, y, argb(eye));
						return eye;
					}
					if (fy == 3 && fx >= 1 && fx <= 8) return goldPal[1];
					if (fx == 4 || fx == 5) {
						if (fy >= 1 && fy <= 2) {
							g.setRGB(x, y, argb(core));
							return core;
						}
					}
				}
				if (fy == 0) return goldPal[2];
				return 0;
			});
			cube(40, 40, 2, 6, 2, crystal, glowAll(1.0));
			cube(56, 0, 6, 16, 6, gold);
			cube(80, 0, 6, 16, 6, gold);
			cube(64, 40, 8, 8, 8, crystal, glowAll(0.5));
			cube(48, 60, 8, 5, 8, gold, (g, x, y, face, fx, fy, fw, fh) -> fy == fh - 1 ? core : 0);
			cube(100, 24, 3, 10, 3, crystal, glowAll(1.0));
			cube(0, 62, 16, 1, 1, new Mat(new int[]{0xfff4b8, 0xffffff}, "crystal"), glowAll(1.0));
			end(name);
		}

		static void tyrant() throws IOException {
			begin(128, 128, 506);
			Mat fur = new Mat(new int[]{0x07040c, 0x100a1a, 0x1a1028, 0x251638, 0x321e4a}, "fur", new int[]{0x9b4ae0, 0xc88bff, 0x7225b8}, 0.025);
			cube(0, 0, 20, 16, 28, fur, (g, x, y, face, fx, fy, fw, fh) -> {
				// glowing void cracks along the flanks
				if ((face == 2 || face == 4) && Math.abs(fy - (fh / 2.0 + Math.sin(fx * 0.5) * 3)) < 0.6) {
					g.setRGB(x, y, argb(0xb36bff));
					return 0xb36bff;
				}
				return 0;
			});
			cube(0, 44, 14, 12, 12, fur, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1) {
					if (fy == 4 && (fx >= 2 && fx <= 4 || fx >= 9 && fx <= 11)) {
						g.setRGB(x, y, argb(0xff5ad6));
						return 0xff5ad6;
					}
					if (fy == 3 && (fx >= 2 && fx <= 4 || fx >= 9 && fx <= 11)) return 0x3e1a5a;
					if (fy >= 9 && fx % 2 == 0 && fx > 1 && fx < 12) return 0xebe4cc;
				}
				return 0;
			});
			cube(52, 44, 12, 4, 10, fur, (g, x, y, face, fx, fy, fw, fh) -> (face == 0 && (fx % 2 == 0) && (fy == 0 || fx == 0 || fx == fw - 1)) ? 0xebe4cc : (face == 0 ? 0x8a1f3a : 0));
			cube(96, 0, 3, 12, 3, new Mat(BONE, "stone"), (g, x, y, face, fx, fy, fw, fh) -> fy < 3 ? 0x2a2a2a : 0);
			cube(108, 0, 3, 9, 3, VOID_CRYSTAL, glowAll(0.6));
			cube(96, 16, 7, 12, 7, fur, (g, x, y, face, fx, fy, fw, fh) -> fy >= fh - 2 && face != 0 && face != 5 && fx % 2 == 0 ? 0xebe4cc : 0);
			cube(0, 68, 6, 6, 16, fur);
			cube(44, 68, 3, 3, 6, VOID_CRYSTAL, glowAll(1.0));
			end("umbral_tyrant");
		}

		static void warden(String name, long seed, int[] ironPal, Mat ember, int[] capePal, int visor, int[] trimPal) throws IOException {
			begin(128, 64, seed);
			Mat iron = new Mat(ironPal, "metal");

			cube(0, 0, 8, 9, 8, iron, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy == 4 && fx >= 1 && fx <= 6) {
					g.setRGB(x, y, argb(visor));
					return visor;
				}
				if (face == 1 && fy == 5 && (fx == 3 || fx == 4)) return ember.pal()[3];
				if (fy == 0) return trimPal[2];
				return 0;
			});
			cube(32, 0, 2, 5, 9, ember, glowAll(0.9));
			cube(0, 17, 10, 12, 6, iron, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && (Math.abs(fx - 4.5) < 0.6 || fy == 5)) {
					g.setRGB(x, y, argb(ember.pal()[3]));
					return ember.pal()[3];
				}
				if (fy == fh - 2) return trimPal[2];
				return 0;
			});
			cube(54, 0, 4, 13, 4, iron);
			cube(70, 0, 4, 13, 4, iron);
			cube(86, 0, 6, 4, 6, new Mat(trimPal, "metal"));
			cube(40, 18, 2, 4, 20, ember, (g, x, y, face, fx, fy, fw, fh) -> {
				g.setRGB(x, y, argb(img.getRGB(x, y) == 0 ? visor : ember.pal()[3]));
				return 0;
			});
			cube(106, 18, 5, 6, 1, new Mat(trimPal, "metal"));
			cube(0, 36, 5, 12, 5, iron);
			cube(20, 36, 5, 12, 5, iron);
			cube(84, 18, 10, 16, 1, new Mat(capePal, "cloth"),
					(g, x, y, face, fx, fy, fw, fh) -> fy >= fh - 2 ? trimPal[2] : 0);
			copyGlowFromImage(40, 18, 44, 24);
			end(name);
		}

		// ------------------------------------------------------------ update 3 creatures
		static final int[] SCULK_P = {0x041418, 0x08262c, 0x0d3a42, 0x135058, 0x1a6a72, 0x2a8a90};
		static final int[] SCULK_GLOW = {0x29dfeb, 0x5cf2ff, 0xb0ffff};

		/** Village guard on the villager/illager UV layout: robe, tabard with Glowcube emblem, painted armor variants. */
		static void guardianVillager(String name, int variant) throws IOException {
			begin(64, 64, 540 + variant);
			int[] skin = {0x9c6b4a, 0xb5815d, 0xc99572, 0xd9a886};
			int[] uniform = {0x1d4a6a, 0x24608a, 0x2f78a8, 0x4290c4};
			int[] chain = {0x4a4e58, 0x6a707c, 0x8a909c, 0xaab0bc};
			int[] iron = {0x7a8290, 0x9aa2b0, 0xbcc4d0, 0xdde3ec};
			int[] armor = variant == 2 ? iron : chain;
			cube(0, 0, 8, 10, 8, new Mat(skin, "stone"), (g, x, y, face, fx, fy, fw, fh) -> {
				if (variant == 2 && fy <= 3) return armor[fx % 2 == 0 ? 3 : 2]; // iron helmet
				if (fy <= 1) return 0x3b2614;
				if (face == 1) {
					if (fy == 5 && (fx == 1 || fx == 6)) return 0xffffff;
					if (fy == 5 && (fx == 2 || fx == 5)) return 0x2a6a3a;
					if (fy == 4 && fx >= 1 && fx <= 6 && fx != 3 && fx != 4) return 0x3b2614;
				}
				return 0;
			});
			cube(24, 0, 2, 4, 2, new Mat(new int[]{0xb5815d, 0xc99572}, "stone"));
			Detail tabard = (g, x, y, face, fx, fy, fw, fh) -> {
				if (variant >= 1 && fy <= 5) return armor[(fx + fy) % 4 == 0 ? 3 : 1 + (fx + fy) % 2];
				if (face == 1 || face == 3) {
					int cx = fx - 2, cy = fy - 6;
					if (cx >= 0 && cx < 4 && cy >= 0 && cy < 4) {
						boolean edge = cx == 0 || cy == 0 || cx == 3 || cy == 3;
						return edge ? 0x0b2a3a : (cx + cy) % 2 == 0 ? 0x9ff2ff : 0x5cdfff;
					}
				}
				if (fy == fh - 2) return 0x4a3220;
				if (fx == 0 || fx == fw - 1) return 0xd19a0f;
				return 0;
			};
			cube(16, 20, 8, 12, 6, new Mat(uniform, "cloth"), tabard);
			cube(0, 38, 8, 20, 6, new Mat(uniform, "cloth"), (g, x, y, face, fx, fy, fw, fh) -> {
				if (variant >= 1 && fy <= 5) return armor[1 + (fx + fy) % 3];
				if (fy >= fh - 2) return 0xd19a0f;
				if (fx == fw / 2 && face == 1) return 0xd19a0f;
				return 0;
			});
			Detail sleeve = (g, x, y, face, fx, fy, fw, fh) -> fy >= fh - 3 ? skin[2] : (variant >= 1 && fy < 3 ? armor[2] : 0);
			cube(44, 22, 4, 8, 4, new Mat(uniform, "cloth"), sleeve);
			cube(40, 38, 8, 4, 4, new Mat(uniform, "cloth"));
			cube(40, 46, 4, 12, 4, new Mat(uniform, "cloth"), sleeve);
			cube(0, 22, 4, 12, 4, new Mat(new int[]{0x2a2a3a, 0x34344a, 0x40405a}, "cloth"), (g, x, y, face, fx, fy, fw, fh) -> fy >= 9 ? 0x3b2614 : 0);
			save(img, "entity/" + name);
		}

		/** Realm merchant on the villager/illager UV layout: hooded robe in the realm colours, gem brooch, belt with pouches. */
		static void trader(String name, int[] robe, int[] gem, int trim) throws IOException {
			begin(64, 64, name.hashCode());
			int[] skin = {0x9c6b4a, 0xb5815d, 0xc99572, 0xd9a886};
			cube(0, 0, 8, 10, 8, new Mat(skin, "stone"), (g, x, y, face, fx, fy, fw, fh) -> {
				// hood over the head, face left open on the front
				boolean faceArea = face == 1 && fx >= 1 && fx <= 6 && fy >= 3;
				if (!faceArea) return fy == 0 || (face != 1 && fy == 9) ? trim : robe[(fx + fy) % 3 + 1];
				if (fy == 5 && (fx == 1 || fx == 6)) return 0xffffff;
				if (fy == 5 && (fx == 2 || fx == 5)) {
					g.setRGB(x, y, argb(gem[3]));
					return gem[3];
				}
				if (fy == 8 && fx >= 2 && fx <= 5) return 0x7a4a30;
				return 0;
			});
			cube(24, 0, 2, 4, 2, new Mat(new int[]{0xb5815d, 0xc99572}, "stone"));
			Detail coat = (g, x, y, face, fx, fy, fw, fh) -> {
				if (fy == 7) return 0x4a3220;
				if (fy == 8 && face == 1 && (fx == 1 || fx == 6)) return 0x7a5232;
				if (face == 1 && fy == 2 && (fx == 3 || fx == 4)) {
					g.setRGB(x, y, argb(gem[4]));
					return gem[4];
				}
				if (face == 1 && (fx == 3 || fx == 4)) return trim;
				return 0;
			};
			cube(16, 20, 8, 12, 6, new Mat(robe, "cloth"), coat);
			cube(0, 38, 8, 20, 6, new Mat(robe, "cloth"), (g, x, y, face, fx, fy, fw, fh) -> {
				if (fy >= fh - 2) return trim;
				if (fy == 7) return 0x4a3220;
				if (face == 1 && fx == fw / 2) return trim;
				if (noise(x, y) > 0.92) {
					g.setRGB(x, y, argb(gem[2]));
					return gem[2];
				}
				return 0;
			});
			Detail sleeve = (g, x, y, face, fx, fy, fw, fh) -> fy >= fh - 3 ? skin[2] : fy == fh - 4 ? trim : 0;
			cube(44, 22, 4, 8, 4, new Mat(robe, "cloth"), sleeve);
			cube(40, 38, 8, 4, 4, new Mat(robe, "cloth"));
			cube(40, 46, 4, 12, 4, new Mat(robe, "cloth"), sleeve);
			cube(0, 22, 4, 12, 4, new Mat(new int[]{0x2a2a3a, 0x34344a, 0x40405a}, "cloth"), (g, x, y, face, fx, fy, fw, fh) -> fy >= 9 ? 0x3b2614 : 0);
			end(name);
		}

		static double noise(int x, int y) {
			long h = x * 3129871L ^ y * 116129781L;
			h = h * h * 42317861L + h * 11L;
			return ((h >> 16) & 0xFFFF) / 65535.0;
		}

		static void creatures3() throws IOException {
			// Echo Warden (128x128)
			begin(128, 128, 560);
			Mat hide = new Mat(SCULK_P, "fur", SCULK_GLOW, 0.035);
			cube(0, 0, 18, 21, 11, hide, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy > 3 && fy < 14 && fx > 4 && fx < 13 && (fy % 3 == 0 || Math.abs(fx - 8.5) < 0.6)) {
					g.setRGB(x, y, argb(0x5cf2ff));
					return 0x5cf2ff;
				}
				return 0;
			});
			cube(0, 32, 16, 16, 10, hide, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy >= 7 && fy <= 8 && (fx >= 3 && fx <= 5 || fx >= 10 && fx <= 12)) {
					g.setRGB(x, y, argb(0xb0ffff));
					return 0xb0ffff;
				}
				if (face == 1 && fy >= 11 && fx % 2 == 0 && fx > 2 && fx < 13) return 0x0a0a0a;
				return 0;
			});
			cube(60, 40, 16, 16, 0, new Mat(new int[]{0x135058, 0x1a6a72, 0x29dfeb}, "crystal"), glowAll(0.4));
			cube(60, 56, 16, 16, 0, new Mat(new int[]{0x135058, 0x1a6a72, 0x29dfeb}, "crystal"), glowAll(0.4));
			cube(58, 0, 8, 28, 8, hide);
			cube(90, 0, 8, 28, 8, hide);
			cube(0, 60, 6, 13, 6, hide);
			cube(24, 60, 6, 13, 6, hide);
			end("echo_warden");

			// Sculk Stalker (humanoid 64x64)
			begin(64, 64, 561);
			Mat flesh = new Mat(new int[]{0x1a2a2a, 0x24383a, 0x30484a, 0x3c5a5c}, "fur", SCULK_GLOW, 0.05);
			cube(0, 0, 8, 8, 8, flesh, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy == 4 && (fx == 2 || fx == 5)) {
					g.setRGB(x, y, argb(0x5cf2ff));
					return 0x5cf2ff;
				}
				return 0;
			});
			cube(16, 16, 8, 12, 4, flesh);
			cube(40, 16, 4, 12, 4, flesh);
			cube(0, 16, 4, 12, 4, flesh);
			end("sculk_stalker");

			// Glimmer Deer (64x64)
			begin(64, 64, 562);
			Mat coat = new Mat(new int[]{0x6a5a8a, 0x806ea4, 0x9886be, 0xb0a0d6, 0xc8b8ec}, "fur", new int[]{0xffffff, 0xe0f8ff}, 0.03);
			cube(0, 0, 8, 8, 16, coat, (g, x, y, face, fx, fy, fw, fh) -> face == 5 ? 0xe8e0f8 : 0);
			cube(0, 24, 4, 9, 4, coat);
			cube(16, 24, 6, 5, 8, coat, (g, x, y, face, fx, fy, fw, fh) -> {
				if ((face == 2 || face == 4) && fy == 1 && fx == 2) return 0x101018;
				if (face == 1 && fy >= 3) return 0x3a2a4a;
				return 0;
			});
			cube(44, 24, 1, 8, 6, new Mat(GLOWCRYSTAL, "crystal"), glowAll(0.9));
			cube(0, 37, 3, 2, 1, coat);
			cube(0, 40, 2, 10, 2, coat, (g, x, y, face, fx, fy, fw, fh) -> fy >= 8 ? 0x2a2030 : 0);
			cube(8, 40, 2, 10, 2, coat, (g, x, y, face, fx, fy, fw, fh) -> fy >= 8 ? 0x2a2030 : 0);
			cube(16, 40, 2, 3, 2, new Mat(new int[]{0xe8e0f8, 0xffffff}, "fur"));
			end("glimmer_deer");

			// Cloud Bunny (64x32)
			begin(64, 32, 563);
			Mat fluff = new Mat(new int[]{0xc8d8ec, 0xd8e6f4, 0xe8f2fa, 0xffffff}, "fur");
			cube(0, 0, 6, 6, 8, fluff);
			cube(0, 14, 5, 5, 5, fluff, (g, x, y, face, fx, fy, fw, fh) -> {
				if (face == 1 && fy == 1 && (fx == 1 || fx == 3)) return 0x2a3a6a;
				if (face == 1 && fy == 3 && fx == 2) return 0xffa0c0;
				return 0;
			});
			cube(20, 14, 2, 6, 1, fluff, (g, x, y, face, fx, fy, fw, fh) -> face == 1 && fy > 0 && fy < 5 ? 0xffc8dc : 0);
			cube(0, 24, 2, 3, 4, fluff);
			cube(12, 24, 5, 3, 2, fluff);
			cube(28, 0, 3, 3, 2, new Mat(new int[]{0xffffff}, "fur"));
			save(img, "entity/cloud_bunny");

			// Shade Toad (64x32)
			begin(64, 32, 564);
			Mat skinT = new Mat(new int[]{0x0e2a2a, 0x143c3a, 0x1a504c, 0x22665e}, "fur", new int[]{0x5cf2dd, 0x2bbfa9}, 0.06);
			cube(0, 0, 10, 6, 11, skinT, (g, x, y, face, fx, fy, fw, fh) -> face == 5 ? 0x8ac8a8 : 0);
			cube(42, 0, 3, 2, 3, new Mat(new int[]{0xfff4b8, 0xffe066}, "crystal"), (g, x, y, face, fx, fy, fw, fh) -> {
				g.setRGB(x, y, argb(0xffe066));
				return face == 1 && fx == 1 ? 0x101010 : 0;
			});
			cube(42, 6, 6, 2, 3, new Mat(new int[]{0x8ac8a8, 0xa8e0c0}, "fur"));
			cube(0, 17, 4, 3, 6, skinT);
			cube(20, 17, 2, 3, 2, skinT);
			end("shade_toad");

			// Lantern Bug (wisp layout 32x32)
			begin(32, 32, 565);
			cube(0, 0, 6, 6, 6, new Mat(new int[]{0x3a2a10, 0x5a4018, 0x7a5a20}, "stone"), (g, x, y, face, fx, fy, fw, fh) -> {
				if (fy >= 3) {
					g.setRGB(x, y, argb(fy == 3 ? 0xffd060 : 0xfff0a0));
					return fy == 3 ? 0xffd060 : 0xfff0a0;
				}
				if (face == 1 && fy == 1 && (fx == 1 || fx == 4)) return 0x101010;
				return 0;
			});
			cube(0, 12, 2, 2, 2, new Mat(new int[]{0xc8e8ff, 0xffffff}, "crystal"));
			cube(8, 12, 2, 4, 2, new Mat(new int[]{0xffe066, 0xfff0a0}, "crystal"), glowAll(1.0));
			end("lantern_bug");

			// Sculk Snail (64x64)
			begin(64, 64, 566);
			Mat foot = new Mat(new int[]{0x1a3a40, 0x24505a, 0x306a74}, "fur");
			cube(0, 0, 6, 3, 14, foot);
			cube(0, 17, 8, 8, 8, new Mat(SCULK_P, "stone"), (g, x, y, face, fx, fy, fw, fh) -> {
				double d = Math.hypot(fx - fw / 2.0 + 0.5, fy - fh / 2.0 + 0.5);
				if ((face == 2 || face == 4) && Math.abs(d - 2.5) < 0.6) {
					g.setRGB(x, y, argb(0x5cf2ff));
					return 0x5cf2ff;
				}
				return 0;
			});
			cube(40, 0, 1, 5, 1, foot, (g, x, y, face, fx, fy, fw, fh) -> {
				if (fy == 0) {
					g.setRGB(x, y, argb(0xb0ffff));
					return 0xb0ffff;
				}
				return 0;
			});
			end("sculk_snail");

			// Ember Salamander (64x64)
			begin(64, 64, 567);
			Mat scalesS = new Mat(new int[]{0x4a1a08, 0x7a2a0a, 0xa83c10, 0xd05a1a}, "fur", new int[]{0xffb84d, 0xffd080}, 0.06);
			cube(0, 0, 6, 4, 14, scalesS, (g, x, y, face, fx, fy, fw, fh) -> face == 0 && fx % 3 == 0 ? 0x2a0a04 : 0);
			cube(0, 18, 5, 4, 6, scalesS, (g, x, y, face, fx, fy, fw, fh) -> {
				if ((face == 2 || face == 4) && fy == 1 && fx == 4) {
					g.setRGB(x, y, argb(0xffe066));
					return 0xffe066;
				}
				return 0;
			});
			cube(22, 18, 3, 3, 12, new Mat(EMBER, "crystal"), glowAll(0.6));
			cube(40, 0, 4, 2, 2, scalesS);
			end("ember_salamander");

			// Void Jelly (64x64)
			begin(64, 64, 568);
			Mat bell = new Mat(new int[]{0x3a1a5a, 0x5a2a8a, 0x8a4ac0, 0xb07ae0, 0xd8b0ff}, "crystal", new int[]{0xf0d9ff, 0xffffff}, 0.08);
			cube(0, 0, 12, 7, 12, bell, glowAll(0.3));
			cube(0, 19, 8, 3, 8, new Mat(new int[]{0xff7aff, 0xffb0ff}, "crystal"), glowAll(1.0));
			cube(48, 0, 1, 14, 1, new Mat(new int[]{0x8a4ac0, 0xd8b0ff}, "crystal"), glowAll(0.5));
			end("void_jelly");

			icon("boss_echo_warden", new int[]{0x0d3a42, 0x135058, 0x1a6a72, 0x29dfeb, 0x5cf2ff, 0xb0ffff}, SCULK_P);
		}

		static void mapIcons() throws IOException {
			// player arrow (points up)
			BufferedImage p = img(16, 16);
			for (int y = 1; y < 15; y++) {
				int half = (y - 1) / 2;
				for (int x = 8 - half; x <= 7 + half; x++) {
					if (y > 11 && Math.abs(x - 7.5) < (y - 11) * 1.5) continue;
					p.setRGB(x, y, argb(y < 5 ? 0xffffff : 0x9ff2ff));
				}
			}
			outline(p, 0x0b0414);
			saveTo(p, "gui/map/player");
			icon("boss_glowkeeper", GLOWCRYSTAL, GOLD);
			icon("boss_umbral_tyrant", VOIDSHARD, SHADOW_STEEL);
			icon("boss_ember_warden", EMBER, GOLD);
			icon("boss_defeated", new int[]{0x404040, 0x606060, 0x808080, 0xa0a0a0, 0xc0c0c0, 0xe0e0e0}, new int[]{0x303030, 0x505050, 0x707070, 0x909090, 0xb0b0b0, 0xd0d0d0});
		}

		/** Skull-shaped boss marker. */
		static void icon(String name, int[] main, int[] rim) throws IOException {
			BufferedImage b = img(16, 16);
			for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
				double dx = x - 7.5, dy = y - 6.5;
				boolean skull = dx * dx / 36 + dy * dy / 30 <= 1 && y < 12;
				boolean jaw = y >= 10 && y <= 13 && Math.abs(dx) < 3.6;
				if (skull || jaw) b.setRGB(x, y, argb(main[clamp(4 - (int) (Math.hypot(dx + 2, dy + 2) / 2.5), 1, 5)]));
			}
			for (int[] e : new int[][]{{5, 6}, {6, 6}, {5, 7}, {6, 7}, {9, 6}, {10, 6}, {9, 7}, {10, 7}}) b.setRGB(e[0], e[1], argb(0x0b0414));
			b.setRGB(7, 9, argb(0x0b0414));
			b.setRGB(8, 9, argb(0x0b0414));
			for (int x = 5; x <= 10; x += 2) b.setRGB(x, 12, argb(0x0b0414));
			outline(b, darken(rim[0], 0.5));
			saveTo(b, "gui/map/" + name);
		}

		static void saveTo(BufferedImage i, String path) throws IOException {
			save(i, path);
		}
	}

	// ================================================================ BLOCKS
	static void blocks() throws IOException {
		BufferedImage skystone = stone(SKYSTONE, 11, 0.06);
		save(skystone, "block/skystone");
		save(bricks(SKYSTONE, 12), "block/skystone_bricks");
		save(chiseled(SKYSTONE, GLOWCRYSTAL, 13), "block/chiseled_skystone");
		BufferedImage soil = dirt(LUMEN_SOIL, 14);
		save(soil, "block/lumen_soil");
		save(grassTop(LUMEN_GRASS, 15), "block/lumen_grass_top");
		save(grassSide(soil, LUMEN_GRASS, 16), "block/lumen_grass_side");
		save(ore(skystone, GLOWCRYSTAL, 17), "block/glowcrystal_ore");
		save(crystal(GLOWCRYSTAL, 18), "block/glowcrystal_block");
		save(logSide(AURORA_BARK, 19), "block/aurora_log");
		save(logTop(AURORA_BARK, AURORA_WOOD, 20), "block/aurora_log_top");
		save(planks(AURORA_WOOD, 21), "block/aurora_planks");
		save(leaves(AURORA_LEAF, 22), "block/aurora_leaves");
		save(flower(LUMEN_GRASS, GLOWCRYSTAL, 23), "block/lumen_bloom");
		saveAnimated(portal(new int[]{0x0a3d66, 0x1477b3, 0x2fb4e6, 0x8ae6ff, 0xe6fbff}, 24, 32), "block/lumen_portal", 2);

		BufferedImage umbral = stone(UMBRAL_STONE, 31, 0.08);
		save(umbral, "block/umbral_stone");
		save(bricks(UMBRAL_STONE, 32), "block/umbral_bricks");
		save(chiseled(UMBRAL_STONE, VOIDSHARD, 33), "block/chiseled_umbral_stone");
		save(mossTop(UMBRAL_MOSS, GLOW_SPOT, 34), "block/umbral_moss_top");
		save(grassSide(umbral, UMBRAL_MOSS, 35), "block/umbral_moss_side");
		save(ore(umbral, VOIDSHARD, 36), "block/voidshard_ore");
		save(crystal(VOIDSHARD, 37), "block/voidshard_block");
		save(shadecap(SHADECAP, GLOW_SPOT, 38), "block/shadecap_block");
		save(stem(new int[]{0x5c5470, 0x766d8c, 0x8f87a6, 0xaba4c0}, 39), "block/shadecap_stem");
		save(flower(UMBRAL_MOSS, VOIDSHARD, 40), "block/voidbloom");
		saveAnimated(portal(new int[]{0x12041f, 0x3a0d66, 0x6e1fb3, 0xb36bff, 0xf0d9ff}, 41, 32), "block/umbral_portal", 2);

		save(altar(SKYSTONE, GOLD, GLOWCRYSTAL, 50, true), "block/glowkeeper_altar_top");
		save(altar(SKYSTONE, GOLD, GLOWCRYSTAL, 51, false), "block/glowkeeper_altar_side");
		save(altar(UMBRAL_STONE, SHADOW_STEEL, VOIDSHARD, 52, true), "block/tyrant_altar_top");
		save(altar(UMBRAL_STONE, SHADOW_STEEL, VOIDSHARD, 53, false), "block/tyrant_altar_side");
		save(altar(SHADOW_STEEL, GOLD, EMBER, 54, true), "block/warden_altar_top");
		save(altar(SHADOW_STEEL, GOLD, EMBER, 55, false), "block/warden_altar_side");
	}

	static BufferedImage stone(int[] pal, long seed, double crackChance) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2}, new double[]{0.55, 0.3, 0.15});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int i = (int) (n[x][y] * (pal.length - 1.2)) + 1;
			img.setRGB(x, y, argb(pal[clamp(i, 0, pal.length - 2)]));
		}
		// cracks
		for (int c = 0; c < 3; c++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			int len = 3 + rng.nextInt(4);
			for (int s = 0; s < len; s++) {
				img.setRGB(x & 15, y & 15, argb(pal[0]));
				if (rng.nextBoolean()) x += rng.nextBoolean() ? 1 : -1; else y += 1;
			}
		}
		// highlights
		for (int i = 0; i < 6; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[pal.length - 1]));
		return img;
	}

	static BufferedImage bricks(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{4, 2}, new double[]{0.7, 0.3});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int row = y / 4, ly = y % 4;
			int off = (row % 2) * 4;
			int lx = (x + off) % 8;
			int c;
			if (ly == 3 || lx == 7) c = pal[0];
			else if (ly == 0 || lx == 0) c = pal[pal.length - 2];
			else if (ly == 2 || lx == 6) c = pal[2];
			else c = pal[clamp(2 + (int) (n[x][y] * 2.2), 0, pal.length - 2)];
			img.setRGB(x, y, argb(c));
		}
		return img;
	}

	static BufferedImage chiseled(int[] pal, int[] gem, long seed) {
		BufferedImage img = bricks(pal, seed);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			boolean border = x == 0 || y == 0 || x == 15 || y == 15;
			boolean inner = x == 2 || y == 2 || x == 13 || y == 13;
			if (border) img.setRGB(x, y, argb(pal[0]));
			else if (x == 1 || y == 1) img.setRGB(x, y, argb(pal[4]));
			else if (x == 14 || y == 14) img.setRGB(x, y, argb(pal[1]));
			else if (inner && x >= 2 && y >= 2 && x <= 13 && y <= 13) img.setRGB(x, y, argb(pal[1]));
			else if (x > 2 && y > 2 && x < 13 && y < 13) img.setRGB(x, y, argb(pal[3]));
		}
		// central rune: diamond
		for (int y = 3; y < 13; y++) for (int x = 3; x < 13; x++) {
			double d = Math.abs(x - 7.5) + Math.abs(y - 7.5);
			if (d < 1.6) img.setRGB(x, y, argb(gem[5]));
			else if (d < 2.6) img.setRGB(x, y, argb(gem[4]));
			else if (d < 3.6) img.setRGB(x, y, argb(gem[2]));
			else if (d < 4.4) img.setRGB(x, y, argb(pal[1]));
		}
		return img;
	}

	static BufferedImage dirt(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{4, 2, 1}, new double[]{0.5, 0.3, 0.2});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
			img.setRGB(x, y, argb(pal[clamp((int) (n[x][y] * pal.length), 0, pal.length - 1)]));
		for (int i = 0; i < 10; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[0]));
		for (int i = 0; i < 5; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[pal.length - 1]));
		return img;
	}

	static BufferedImage grassTop(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2, 1}, new double[]{0.35, 0.3, 0.2, 0.15});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
			img.setRGB(x, y, argb(pal[clamp((int) (n[x][y] * (pal.length - 1)), 0, pal.length - 2)]));
		// little blades
		for (int i = 0; i < 14; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(pal[4]));
			img.setRGB(x, (y + 1) & 15, argb(pal[1]));
		}
		for (int i = 0; i < 4; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[pal.length - 1]));
		return img;
	}

	static BufferedImage mossTop(int[] pal, int[] glow, long seed) {
		BufferedImage img = grassTop(pal, seed);
		rng = new Random(seed * 7);
		for (int i = 0; i < 7; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(glow[2 + rng.nextInt(2)]));
			if (rng.nextBoolean()) img.setRGB((x + 1) & 15, y, argb(glow[0]));
		}
		return img;
	}

	static BufferedImage grassSide(BufferedImage base, int[] grass, long seed) {
		rng = new Random(seed);
		BufferedImage img = copy(base);
		for (int x = 0; x < 16; x++) {
			int depth = 3 + rng.nextInt(3) + (x % 5 == 0 ? 1 : 0);
			for (int y = 0; y < depth; y++) {
				int c = y == depth - 1 ? grass[0] : grass[clamp(grass.length - 2 - y + rng.nextInt(2), 1, grass.length - 2)];
				img.setRGB(x, y, argb(c));
			}
			if (rng.nextInt(4) == 0) img.setRGB(x, depth, argb(grass[0]));
		}
		return img;
	}

	static BufferedImage ore(BufferedImage base, int[] gem, long seed) {
		rng = new Random(seed);
		BufferedImage img = copy(base);
		int[][] spots = {{3, 3}, {11, 2}, {7, 8}, {2, 12}, {12, 11}};
		for (int[] s : spots) {
			int cx = s[0] + rng.nextInt(2), cy = s[1] + rng.nextInt(2);
			int size = 1 + rng.nextInt(2);
			for (int dy = -size; dy <= size; dy++) for (int dx = -size; dx <= size; dx++) {
				if (Math.abs(dx) + Math.abs(dy) > size + 0.5) continue;
				int x = cx + dx, y = cy + dy;
				if (x < 0 || y < 0 || x > 15 || y > 15) continue;
				int c = (dx == 0 && dy == 0) ? gem[5] : (dx + dy < 0 ? gem[4] : gem[2]);
				img.setRGB(x, y, argb(c));
			}
			// dark rim under crystal
			int bx = cx + size, by = cy + 1;
			if (bx < 16 && by < 16) img.setRGB(bx, by, argb(gem[0]));
		}
		return img;
	}

	static BufferedImage crystal(int[] pal, long seed) {
		rng = new Random(seed);
		int pts = 7;
		double[][] p = new double[pts][2];
		for (int i = 0; i < pts; i++) { p[i][0] = rng.nextDouble() * 16; p[i][1] = rng.nextDouble() * 16; }
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double best = 1e9, second = 1e9; int bi = 0;
			for (int i = 0; i < pts; i++) for (int ox = -16; ox <= 16; ox += 16) for (int oy = -16; oy <= 16; oy += 16) {
				double d = Math.hypot(x + 0.5 - p[i][0] - ox, y + 0.5 - p[i][1] - oy);
				if (d < best) { second = best; best = d; bi = i; } else if (d < second) second = d;
			}
			int c;
			if (second - best < 0.9) c = pal[pal.length - 2];
			else {
				double facet = ((bi * 37) % pts) / (double) pts;
				double grad = (x - y) / 32.0;
				c = pal[clamp((int) ((facet * 0.7 + 0.3 + grad) * (pal.length - 2)), 0, pal.length - 3)];
			}
			img.setRGB(x, y, argb(c));
		}
		for (int i = 0; i < 5; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[pal.length - 1]));
		return img;
	}

	static BufferedImage logSide(int[] pal, long seed) {
		rng = new Random(seed);
		BufferedImage img = img(16, 16);
		double[] col = new double[16];
		for (int x = 0; x < 16; x++) col[x] = rng.nextDouble();
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double v = col[x] * 0.6 + 0.4 * Math.sin((y + x * 3) * 0.7) * 0.5 + 0.2;
			img.setRGB(x, y, argb(pal[clamp((int) (v * pal.length), 1, pal.length - 1)]));
		}
		for (int f = 0; f < 4; f++) {
			int x = rng.nextInt(16), y = rng.nextInt(16), len = 4 + rng.nextInt(6);
			for (int i = 0; i < len; i++) img.setRGB(x, (y + i) & 15, argb(pal[0]));
		}
		// glowing sap veins
		for (int i = 0; i < 3; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(0x7ff5da));
			img.setRGB(x, (y + 1) & 15, argb(0x2bbfa9));
		}
		return img;
	}

	static BufferedImage logTop(int[] bark, int[] wood, long seed) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
			if (edge) { img.setRGB(x, y, argb(bark[(x + y) % 2 == 0 ? 1 : 2])); continue; }
			double d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)) + Math.hypot(x - 7.5, y - 7.5) * 0.3;
			int ring = (int) d;
			int c = ring % 2 == 0 ? wood[3] : wood[1];
			if (ring == 0) c = 0x7ff5da;
			img.setRGB(x, y, argb(c));
		}
		return img;
	}

	static BufferedImage planks(int[] pal, long seed) {
		rng = new Random(seed);
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int board = y / 4, ly = y % 4;
			int seam = (board % 2 == 0) ? 11 : 4;
			int c;
			if (ly == 3) c = pal[0];
			else if (x == seam) c = pal[1];
			else {
				double g = Math.sin(x * 0.9 + board * 2.1) * 0.5 + 0.5;
				c = pal[clamp(1 + (int) (g * 2.5) + (ly == 0 ? 1 : 0), 1, pal.length - 1)];
			}
			img.setRGB(x, y, argb(c));
		}
		return img;
	}

	static BufferedImage leaves(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{4, 2, 1}, new double[]{0.4, 0.35, 0.25});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double v = n[x][y];
			if (v < 0.22) continue;
			img.setRGB(x, y, argb(pal[clamp((int) (v * (pal.length - 1)), 0, pal.length - 3)]));
		}
		for (int i = 0; i < 6; i++) {
			int x = rng.nextInt(15), y = rng.nextInt(15);
			img.setRGB(x, y, argb(pal[pal.length - 1]));
			img.setRGB(x + 1, y, argb(pal[pal.length - 2]));
			img.setRGB(x, y + 1, argb(pal[pal.length - 2]));
		}
		return img;
	}

	static BufferedImage flower(int[] stemPal, int[] petal, long seed) {
		BufferedImage img = img(16, 16);
		for (int y = 7; y < 16; y++) img.setRGB(7 + (y > 12 ? 1 : 0), y, argb(stemPal[y % 2 == 0 ? 1 : 2]));
		img.setRGB(6, 11, argb(stemPal[3])); img.setRGB(5, 10, argb(stemPal[3]));
		img.setRGB(9, 13, argb(stemPal[3])); img.setRGB(10, 12, argb(stemPal[3]));
		int cx = 7, cy = 4;
		for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
			double d = Math.hypot(dx, dy);
			double ang = Math.atan2(dy, dx);
			double petalR = 2.2 + Math.cos(ang * 5) * 1.0;
			if (d <= petalR) {
				int c = d < 1 ? petal[5] : (d < 1.8 ? petal[4] : petal[2 + ((dx + dy) & 1)]);
				img.setRGB(cx + dx, cy + dy, argb(c));
			}
		}
		return img;
	}

	static BufferedImage shadecap(int[] pal, int[] glow, long seed) {
		rng = new Random(seed);
		BufferedImage img = dirt(pal, seed);
		int[][] spots = {{3, 3, 2}, {11, 4, 2}, {6, 10, 3}, {13, 12, 1}, {1, 13, 1}};
		for (int[] s : spots) {
			for (int dy = -s[2]; dy <= s[2]; dy++) for (int dx = -s[2]; dx <= s[2]; dx++) {
				double d = Math.hypot(dx, dy);
				if (d > s[2] + 0.3) continue;
				int c = d < 0.8 ? glow[3] : (d < s[2] - 0.4 ? glow[2] : glow[0]);
				img.setRGB((s[0] + dx) & 15, (s[1] + dy) & 15, argb(c));
			}
		}
		return img;
	}

	static BufferedImage stem(int[] pal, long seed) {
		rng = new Random(seed);
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double v = 0.5 + 0.35 * Math.sin(x * 1.3 + rng.nextDouble() * 0.6);
			img.setRGB(x, y, argb(pal[clamp((int) (v * pal.length), 0, pal.length - 1)]));
		}
		return img;
	}

	static BufferedImage[] portal(int[] pal, long seed, int frames) {
		BufferedImage[] out = new BufferedImage[frames];
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2}, new double[]{0.5, 0.3, 0.2});
		for (int f = 0; f < frames; f++) {
			BufferedImage img = img(16, 16);
			double t = f / (double) frames * Math.PI * 2;
			for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
				double dx = x - 7.5, dy = y - 7.5;
				double r = Math.hypot(dx, dy);
				double a = Math.atan2(dy, dx);
				double v = Math.sin(a * 3 + r * 0.9 - t) * 0.5 + 0.5;
				v = v * 0.65 + n[x][y] * 0.35 + Math.sin(t + n[x][y] * 6) * 0.08;
				int c = pal[clamp((int) (v * pal.length), 0, pal.length - 1)];
				int alpha = 170 + (int) (v * 70);
				img.setRGB(x, y, (alpha << 24) | (c & 0xFFFFFF));
			}
			out[f] = img;
		}
		return out;
	}

	static BufferedImage altar(int[] stone, int[] metal, int[] gem, long seed, boolean top) {
		BufferedImage img = bricks(stone, seed);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			boolean frame = x <= 1 || y <= 1 || x >= 14 || y >= 14;
			if (frame) img.setRGB(x, y, argb(metal[(x == 0 || y == 0) ? 4 : (x == 15 || y == 15) ? 1 : 3]));
		}
		if (top) {
			for (int y = 2; y < 14; y++) for (int x = 2; x < 14; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				if (d < 1.6) img.setRGB(x, y, argb(gem[5]));
				else if (d < 2.8) img.setRGB(x, y, argb(gem[3]));
				else if (d > 4.2 && d < 5.2) img.setRGB(x, y, argb(metal[2 + ((x + y) & 1)]));
			}
		} else {
			for (int y = 4; y < 12; y++) {
				img.setRGB(7, y, argb(gem[y == 7 || y == 8 ? 5 : 3]));
				img.setRGB(8, y, argb(gem[y == 7 || y == 8 ? 4 : 2]));
			}
			for (int x = 4; x < 12; x++) { img.setRGB(x, 4, argb(metal[3])); img.setRGB(x, 11, argb(metal[2])); }
		}
		return img;
	}

	// ================================================================ ITEMS
	static void items() throws IOException {
		save(shard(GLOWCRYSTAL, 101), "item/glow_shard");
		save(shard(VOIDSHARD, 102), "item/void_shard");
		save(ingot(STARMETAL, 103), "item/starmetal_ingot");
		save(key(GOLD, GLOWCRYSTAL), "item/lumen_key");
		save(key(SHADOW_STEEL, VOIDSHARD), "item/umbral_key");
		save(orb(GLOWCRYSTAL, GOLD, 104), "item/lumen_compass");
		save(orb(VOIDSHARD, SHADOW_STEEL, 105), "item/umbral_compass");
		save(sigil(GOLD, GLOWCRYSTAL), "item/glowkeeper_sigil");
		save(sigil(SHADOW_STEEL, VOIDSHARD), "item/tyrant_sigil");
		save(sigil(EMBER, GOLD), "item/warden_sigil");
		save(core(GLOWCRYSTAL, RADIANT), "item/glowkeeper_core");
		save(heart(VOIDSHARD), "item/tyrant_heart");
		save(core(EMBER, GOLD), "item/ember_heart");
		save(feather(GLOWCRYSTAL), "item/wisp_essence");
		save(fang(BONE, VOIDSHARD), "item/shade_fang");
		save(shard(new int[]{0x3a3f4f, 0x585f75, 0x7a839c, 0x9ea8c2, 0xc7d0e6, 0xffffff}, 106), "item/golem_fragment");

		// tools
		save(sword(GLOWCRYSTAL, GOLD, WOOD, false), "item/glowcrystal_sword");
		save(pickaxe(GLOWCRYSTAL, WOOD), "item/glowcrystal_pickaxe");
		save(axe(GLOWCRYSTAL, WOOD), "item/glowcrystal_axe");
		save(shovel(GLOWCRYSTAL, WOOD), "item/glowcrystal_shovel");
		save(sword(VOIDSHARD, SHADOW_STEEL, SHADOW_STEEL, false), "item/voidshard_sword");
		save(pickaxe(VOIDSHARD, SHADOW_STEEL), "item/voidshard_pickaxe");

		// legendary weapons
		save(sword(RADIANT, GOLD, GLOWCRYSTAL, true), "item/radiant_blade");
		save(scythe(VOIDSHARD, SHADOW_STEEL), "item/void_scythe");
		save(hammer(STARMETAL, GLOWCRYSTAL, WOOD), "item/star_hammer");
		save(dagger(SHADOW_STEEL, VOIDSHARD), "item/shadow_dagger");
		save(bow(AURORA_WOOD, GLOWCRYSTAL, 0), "item/crystal_bow");
		save(bow(AURORA_WOOD, GLOWCRYSTAL, 1), "item/crystal_bow_pulling_0");
		save(bow(AURORA_WOOD, GLOWCRYSTAL, 2), "item/crystal_bow_pulling_1");
		save(bow(AURORA_WOOD, GLOWCRYSTAL, 3), "item/crystal_bow_pulling_2");
		save(sword(EMBER, GOLD, SHADOW_STEEL, true), "item/ember_greatsword");
		save(staff(AURORA_WOOD, GLOWCRYSTAL), "item/aurora_staff");

		// armor
		save(armor(HELMET, GLOWCRYSTAL, GOLD), "item/glowcrystal_helmet");
		save(armor(CHEST, GLOWCRYSTAL, GOLD), "item/glowcrystal_chestplate");
		save(armor(LEGS, GLOWCRYSTAL, GOLD), "item/glowcrystal_leggings");
		save(armor(BOOTS, GLOWCRYSTAL, GOLD), "item/glowcrystal_boots");
		save(armor(HELMET, VOIDSHARD, SHADOW_STEEL), "item/voidshard_helmet");
		save(armor(CHEST, VOIDSHARD, SHADOW_STEEL), "item/voidshard_chestplate");
		save(armor(LEGS, VOIDSHARD, SHADOW_STEEL), "item/voidshard_leggings");
		save(armor(BOOTS, VOIDSHARD, SHADOW_STEEL), "item/voidshard_boots");

		// spawn eggs
		save(egg(0x8ae6ff, 0xffffff), "item/glow_wisp_spawn_egg");
		save(egg(0x7a839c, 0x2fb4e6), "item/crystal_golem_spawn_egg");
		save(egg(0x251c33, 0x9b4ae0), "item/shade_crawler_spawn_egg");
		save(egg(0x6b4a2b, 0xd19a0f), "item/realm_guardian_spawn_egg");
		save(egg(0xffe066, 0x2fb4e6), "item/glowkeeper_spawn_egg");
		save(egg(0x12041f, 0xb36bff), "item/umbral_tyrant_spawn_egg");
		save(egg(0x4a0f05, 0xffb84d), "item/ember_warden_spawn_egg");
	}

	/** Shade every filled cell automatically: lit from top-left, outlined. */
	static BufferedImage shadeMask(char[][] m, int[] mat, int[] mat2, int[] mat3) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			char ch = m[y][x];
			if (ch == '.') continue;
			int[] pal = switch (Character.toLowerCase(ch)) {
				case 'h' -> mat2;
				case 'g' -> mat3;
				default -> mat;
			};
			int shade;
			if (Character.isDigit(ch)) shade = ch - '0';
			else {
				boolean up = filled(m, x, y - 1), left = filled(m, x - 1, y), down = filled(m, x, y + 1), right = filled(m, x + 1, y);
				if (!up || !left) shade = 4;
				else if (!down || !right) shade = 1;
				else shade = 2 + ((x * 7 + y * 3) % 5 == 0 ? 1 : 0);
				if (Character.isUpperCase(ch)) shade = Math.min(shade + 1, 5);
			}
			img.setRGB(x, y, argb(pal[clamp(shade, 0, pal.length - 1)]));
		}
		outline(img, darken(mat[0], 0.55));
		return img;
	}

	static boolean filled(char[][] m, int x, int y) {
		return x >= 0 && y >= 0 && x < 16 && y < 16 && m[y][x] != '.';
	}

	static char[][] grid() {
		char[][] g = new char[16][16];
		for (char[] r : g) java.util.Arrays.fill(r, '.');
		return g;
	}

	static char[][] parse(String... rows) {
		char[][] g = grid();
		for (int y = 0; y < rows.length && y < 16; y++)
			for (int x = 0; x < rows[y].length() && x < 16; x++) g[y][x] = rows[y].charAt(x);
		return g;
	}

	static void set(char[][] g, int x, int y, char c) {
		if (x >= 0 && y >= 0 && x < 16 && y < 16) g[y][x] = c;
	}

	static BufferedImage sword(int[] blade, int[] guard, int[] handle, boolean legendary) {
		char[][] g = grid();
		int tipX = legendary ? 15 : 14, tipY = legendary ? 0 : 1;
		int len = legendary ? 10 : 9;
		for (int t = 0; t < len; t++) {
			int cx = tipX - t, cy = tipY + t;
			set(g, cx, cy, '5');
			if (t > 0) { set(g, cx - 1, cy, '3'); set(g, cx + 1, cy, '1'); }
			if (legendary && t > 1 && t < len - 1) { set(g, cx - 2, cy, '2'); set(g, cx, cy + 1, '4'); }
		}
		int gx = tipX - len, gy = tipY + len;
		for (int i = -2; i <= 2; i++) set(g, gx + i, gy + i, 'g');
		if (legendary) { set(g, gx - 3, gy - 3, 'G'); set(g, gx + 3, gy + 3, 'G'); set(g, gx, gy, 'G'); }
		set(g, gx - 1, gy + 1, 'h'); set(g, gx - 2, gy + 2, 'H'); set(g, gx - 3, gy + 3, 'h');
		set(g, gx - 4, gy + 4, 'g');
		int[] gp = legendary ? guard : guard;
		BufferedImage img = shadeMask(g, blade, handle, gp);
		if (legendary) glowEdge(img, blade[4]);
		return img;
	}

	static BufferedImage pickaxe(int[] head, int[] handle) {
		char[][] g = grid();
		// handle along x+y=15
		for (int y = 4; y <= 14; y++) { int x = 15 - y; set(g, x, y, y % 2 == 0 ? 'H' : 'h'); }
		// arc head
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x - 1.5, dy = 14.5 - y;
			double r = Math.hypot(dx, dy);
			double ang = Math.toDegrees(Math.atan2(dx, dy));
			if (ang < 8 || ang > 82) continue;
			double thick = 1.4 + 0.9 * Math.sin(Math.toRadians((ang - 8) / 74 * 180));
			if (Math.abs(r - 12.3) <= thick) set(g, x, y, r > 12.3 ? 'X' : 'x');
		}
		return shadeMask(g, head, handle, head);
	}

	static BufferedImage axe(int[] head, int[] handle) {
		char[][] g = grid();
		for (int y = 2; y <= 14; y++) { int x = 15 - y; set(g, x, y, y % 2 == 0 ? 'H' : 'h'); }
		// fan-shaped blade on the upper-left side of the handle top
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dist = (15 - x - y) / Math.sqrt(2);
			double along = (x - y) - 6.5;
			if (dist < 0.4 || dist > 5.0) continue;
			if (Math.abs(along) <= 2.0 + dist * 0.75) set(g, x, y, dist > 3.9 ? 'X' : 'x');
		}
		return shadeMask(g, head, handle, head);
	}

	static BufferedImage shovel(int[] head, int[] handle) {
		char[][] g = grid();
		for (int y = 6; y <= 14; y++) { int x = 15 - y; set(g, x, y, y % 2 == 0 ? 'H' : 'h'); }
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.abs(x - 11.0) + Math.abs(y - 4.0) + Math.abs((x - 11.0) - (4.0 - y)) * 0.25;
			if (d <= 4.2) set(g, x, y, d < 2 ? 'X' : 'x');
		}
		set(g, 8, 7, 'g'); set(g, 7, 8, 'g');
		return shadeMask(g, head, handle, GOLD);
	}

	static BufferedImage scythe(int[] blade, int[] handle) {
		char[][] g = grid();
		for (int y = 1; y <= 15; y++) { int x = 16 - y; set(g, x - 1, y, y % 2 == 0 ? 'H' : 'h'); }
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x - 7.5, dy = y - 7.5;
			double r = Math.hypot(dx, dy);
			double ang = Math.toDegrees(Math.atan2(-dy, dx)); // 0 = right, 90 = up
			if (ang < 35 || ang > 175) continue;
			double tAng = (ang - 35) / 140.0; // 0 at handle, 1 at tip
			double thick = 2.2 * (1 - tAng) + 0.4;
			if (r <= 7.4 && r >= 7.4 - thick) set(g, x, y, r > 7.4 - thick / 2 ? 'X' : 'x');
		}
		set(g, 13, 2, 'g'); set(g, 12, 3, 'G');
		BufferedImage img = shadeMask(g, blade, handle, VOIDSHARD);
		glowEdge(img, blade[4]);
		return img;
	}

	static BufferedImage hammer(int[] head, int[] gem, int[] handle) {
		char[][] g = grid();
		for (int y = 5; y <= 15; y++) { int x = 15 - y; set(g, x, y, y % 2 == 0 ? 'H' : 'h'); }
		// big rotated block head centered at (10,4)
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double u = ((x - 10.0) + (y - 4.5)) / Math.sqrt(2);
			double v = ((x - 10.0) - (y - 4.5)) / Math.sqrt(2);
			if (Math.abs(u) <= 5.0 && Math.abs(v) <= 2.9) set(g, x, y, Math.abs(u) > 4.0 ? 'X' : 'x');
		}
		set(g, 10, 4, 'G'); set(g, 10, 5, 'G'); set(g, 9, 4, 'g'); set(g, 11, 5, 'g'); set(g, 9, 5, 'g'); set(g, 11, 4, 'g');
		BufferedImage img = shadeMask(g, head, handle, gem);
		return img;
	}

	static BufferedImage dagger(int[] blade, int[] gem) {
		char[][] g = grid();
		for (int t = 0; t < 6; t++) {
			int cx = 12 - t, cy = 3 + t;
			set(g, cx, cy, '5');
			if (t > 0) { set(g, cx - 1, cy, '3'); set(g, cx + 1, cy, '1'); }
		}
		int gx = 6, gy = 9;
		for (int i = -2; i <= 2; i++) set(g, gx + i, gy + i, 'g');
		set(g, gx - 1, gy + 1, 'h'); set(g, gx - 2, gy + 2, 'h'); set(g, gx - 3, gy + 3, 'G');
		BufferedImage img = shadeMask(g, new int[]{0x2d273b, 0x4a4060, 0x6e6390, 0x9a8fc0, 0xcfc6ee, 0xffffff}, blade, gem);
		glowEdge(img, gem[4]);
		return img;
	}

	static BufferedImage staff(int[] wood, int[] gem) {
		char[][] g = grid();
		for (int y = 5; y <= 15; y++) { int x = 15 - y; set(g, x, y, y % 2 == 0 ? 'H' : 'h'); }
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - 11.5, y - 3.5);
			if (d < 2.2) set(g, x, y, d < 1 ? '5' : 'x');
			else if (d < 3.6 && ((x + y) % 3 == 0)) set(g, x, y, 'g');
		}
		BufferedImage img = shadeMask(g, gem, wood, GOLD);
		glowEdge(img, gem[3]);
		return img;
	}

	static BufferedImage bow(int[] wood, int[] gem, int pull) {
		char[][] g = grid();
		// limb: thick arc around the upper-left corner, ends at (2,13) and (13,2)
		double cx = 13.5 + pull * 0.4, cy = 13.5 + pull * 0.4, rad = 11.2 + pull * 0.3;
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
			double r = Math.hypot(dx, dy);
			double ang = Math.toDegrees(Math.atan2(dy, dx));
			if (ang > -92 || ang < -178) continue;
			double mid = Math.abs(ang + 135) / 45.0; // 0 at grip, 1 at tips
			double thick = 1.6 - mid * 0.7;
			if (Math.abs(r - rad) <= thick) set(g, x, y, mid < 0.18 ? 'g' : (r < rad ? 'h' : 'H'));
		}
		// string from tip to tip, pulled toward the lower-right
		int ax = 2, ay = 13, bx = 13, by = 2;
		for (int i = 0; i <= 30; i++) {
			double t = i / 30.0;
			double off = Math.sin(t * Math.PI) * pull * 1.3;
			int px = (int) Math.round(ax + (bx - ax) * t + off), py = (int) Math.round(ay + (by - ay) * t + off);
			if (filled(g, px, py)) continue;
			set(g, px, py, '5');
		}
		if (pull > 0) {
			// arrow along the diagonal, nock at the string
			int nock = 8 + (int) Math.round(pull * 0.9);
			for (int k = nock; k >= 3; k--) set(g, k, k, k <= 4 ? 'G' : (k >= nock - 1 ? '4' : 'x'));
		}
		int[] stringPal = {0xa8c8e0, 0xc8e2f5, 0xe2f2ff, 0xf2faff, 0xffffff, 0xffffff};
		int[] arrowPal = {0x3b2614, 0x553a1f, 0x6f4e2b, 0x8a663a, 0xd8e8f0, 0xffffff};
		BufferedImage img = shadeMask(g, pull > 0 ? arrowPal : stringPal, wood, gem);
		// keep the string bright even where it was auto shaded
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (g[y][x] == '5') img.setRGB(x, y, argb(0xeef8ff));
		return img;
	}

	static BufferedImage shard(int[] pal, long seed) {
		BufferedImage img = img(16, 16);
		Graphics2D g = img.createGraphics();
		int[][][] crystals = {
				{{7, 1}, {10, 6}, {8, 14}, {5, 7}},
				{{3, 6}, {5, 9}, {4, 14}, {1, 10}},
				{{12, 5}, {14, 9}, {12, 14}, {10, 10}},
		};
		int[] fills = {3, 2, 2};
		for (int c = 0; c < crystals.length; c++) {
			Polygon p = new Polygon();
			for (int[] pt : crystals[c]) p.addPoint(pt[0], pt[1]);
			g.setColor(new Color(pal[fills[c]]));
			g.fillPolygon(p);
		}
		g.dispose();
		// light facets
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			if ((img.getRGB(x, y) >>> 24) == 0) continue;
			if ((img.getRGB(clamp(x - 1, 0, 15), y) >>> 24) == 0) img.setRGB(x, y, argb(pal[4]));
			if ((img.getRGB(clamp(x + 1, 0, 15), y) >>> 24) == 0) img.setRGB(x, y, argb(pal[1]));
		}
		img.setRGB(7, 4, argb(pal[5])); img.setRGB(7, 5, argb(pal[5])); img.setRGB(3, 9, argb(pal[5])); img.setRGB(12, 8, argb(pal[5]));
		outline(img, darken(pal[0], 0.5));
		return img;
	}

	static BufferedImage ingot(int[] pal, long seed) {
		char[][] g = parse(
				"................",
				"................",
				"................",
				"................",
				"................",
				"......XXXXXX....",
				"....XXXxxxxxXX..",
				"..XXxxxxxxxxxx..",
				"..xxxxxxxxxxx...",
				"..xxxxxxxxx.....",
				"..xxxxxxx.......",
				"................");
		return shadeMask(g, pal, pal, pal);
	}

	static BufferedImage key(int[] metal, int[] gem) {
		char[][] g = grid();
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - 4, y - 11);
			if (d <= 3.6 && d >= 2.0) set(g, x, y, 'x');
			if (d < 1.6) set(g, x, y, 'G');
		}
		for (int t = 0; t < 9; t++) { set(g, 6 + t, 9 - t, 'X'); set(g, 7 + t, 9 - t, 'x'); }
		set(g, 12, 5, 'x'); set(g, 13, 6, 'x'); set(g, 10, 3, 'x'); set(g, 11, 4, 'x'); set(g, 14, 7, 'x');
		BufferedImage img = shadeMask(g, metal, metal, gem);
		glowEdge(img, gem[4]);
		return img;
	}

	static BufferedImage orb(int[] glass, int[] frame, long seed) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - 7.5, y - 7.5);
			if (d < 5.2) {
				double l = 1 - Math.hypot(x - 5.5, y - 5.5) / 8.0;
				img.setRGB(x, y, argb(glass[clamp((int) (l * glass.length + 0.5), 0, glass.length - 1)]));
			} else if (d < 6.6) img.setRGB(x, y, argb(frame[(x + y) % 3 == 0 ? 4 : 2]));
		}
		// needle
		for (int i = -3; i <= 3; i++) img.setRGB(8 + i / 2, 8 - i, argb(i > 0 ? 0xff4a6a : 0xffffff));
		outline(img, darken(frame[0], 0.5));
		return img;
	}

	static BufferedImage sigil(int[] metal, int[] gem) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x - 7.5, dy = y - 7.5;
			double d = Math.hypot(dx, dy);
			double a = Math.atan2(dy, dx);
			boolean ray = Math.abs(Math.sin(a * 4)) < 0.25 && d < 7.4;
			if (d < 2.4) img.setRGB(x, y, argb(gem[d < 1.2 ? 5 : 3]));
			else if (d < 3.4) img.setRGB(x, y, argb(metal[1]));
			else if (d < 5.5) img.setRGB(x, y, argb(metal[(dx + dy) < 0 ? 4 : 2]));
			else if (ray) img.setRGB(x, y, argb(gem[4]));
		}
		outline(img, darken(metal[0], 0.5));
		return img;
	}

	static BufferedImage core(int[] glow, int[] ring) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x - 7.5, dy = y - 7.5;
			double d = Math.hypot(dx, dy);
			double ellipse = Math.hypot(dx * 0.45, dy * 1.6);
			if (d < 4.2) img.setRGB(x, y, argb(glow[clamp(5 - (int) d, 0, 5)]));
			else if (Math.abs(ellipse - 3.4) < 0.5) img.setRGB(x, y, argb(ring[3]));
		}
		outline(img, darken(glow[0], 0.4));
		glowEdge(img, glow[3]);
		return img;
	}

	static BufferedImage heart(int[] pal) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double px = (x - 7.5) / 5.6, py = -(y - 7.2) / 5.6;
			double v = Math.pow(px * px + py * py - 1, 3) - px * px * py * py * py;
			if (v <= 0) {
				double l = 1 - Math.hypot(x - 5, y - 5) / 11.0;
				img.setRGB(x, y, argb(pal[clamp((int) (l * 5), 0, 4)]));
			}
		}
		// glowing veins
		int[][] vein = {{7, 5}, {7, 6}, {8, 7}, {8, 8}, {7, 9}, {6, 10}, {9, 9}, {10, 10}, {5, 6}, {4, 6}};
		for (int[] v : vein) img.setRGB(v[0], v[1], argb(0x5cf2dd));
		outline(img, 0x0c0414);
		return img;
	}

	static BufferedImage feather(int[] pal) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - 7.5, y - 7.5);
			double wave = Math.sin(Math.atan2(y - 7.5, x - 7.5) * 6) * 0.8;
			if (d < 3.2 + wave) img.setRGB(x, y, argb(pal[clamp(5 - (int) d, 1, 5)]));
		}
		img.setRGB(7, 7, argb(0xffffff)); img.setRGB(8, 8, argb(0xffffff));
		outline(img, darken(pal[0], 0.6));
		glowEdge(img, pal[3]);
		return img;
	}

	static BufferedImage fang(int[] bone, int[] tip) {
		char[][] g = parse(
				"................",
				"................",
				"..xxxxxx........",
				"...xxxxxxx......",
				"....xxxxxxx.....",
				"......xxxxxx....",
				"........xxxxx...",
				"..........xxx...",
				"...........xg...",
				"............g...",
				"................");
		return shadeMask(g, bone, bone, tip);
	}

	static final int HELMET = 0, CHEST = 1, LEGS = 2, BOOTS = 3;

	static BufferedImage armor(int type, int[] mat, int[] trim) {
		char[][] g = switch (type) {
			case HELMET -> parse(
					"................",
					"................",
					"................",
					"....xxxxxxxx....",
					"...xxxxgGxxxx...",
					"...xxxxxxxxxx...",
					"...xhhhhhhhhx...",
					"...xx......xx...",
					"...xx......xx...",
					"...hh......hh...");
			case CHEST -> parse(
					"................",
					"..xxxx....xxxx..",
					".xxxxxxhhxxxxxx.",
					".xxxxxxxxxxxxxx.",
					".xxxxxxgGxxxxxx.",
					".xxx.xxxxxx.xxx.",
					".xxx.xxggxx.xxx.",
					".hhh.xxxxxx.hhh.",
					".....xxxxxx.....",
					".....hhhhhh.....",
					".....xxxxxx.....",
					"................");
			case LEGS -> parse(
					"................",
					"................",
					"....hhhhhhhh....",
					"....xxxxgxxx....",
					"....xxxxxxxx....",
					"....xxx..xxx....",
					"....xxx..xxx....",
					"....xxx..xxx....",
					"....xxx..xxx....",
					"....hhh..hhh....",
					"....xxx..xxx....",
					"....xxx..xxx....",
					"................");
			default -> parse(
					"................",
					"................",
					"................",
					"................",
					"................",
					"................",
					"................",
					"...xxx....xxx...",
					"...xgx....xgx...",
					"...xxx....xxx...",
					"..hxxx...hxxx...",
					".xxxxx..xxxxx...",
					".hhhhh..hhhhh...",
					"................");
		};
		BufferedImage img = shadeMask(g, mat, trim, GLOWCRYSTAL == mat ? GOLD : mat);
		return img;
	}

	static BufferedImage egg(int base, int spots) {
		BufferedImage img = img(16, 16);
		Random r = new Random(base ^ spots);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double ex = (x - 7.5) / 5.0, ey = (y - 8.5) / (y < 8.5 ? 6.8 : 5.8);
			double d = ex * ex + ey * ey;
			if (d > 1) continue;
			double light = 1 - Math.hypot(x - 5.5, y - 5.0) / 10.0;
			img.setRGB(x, y, argb(shadeColor(base, 0.6 + light * 0.6)));
		}
		for (int i = 0; i < 8; i++) {
			int x = 4 + r.nextInt(8), y = 3 + r.nextInt(11);
			if ((img.getRGB(x, y) >>> 24) == 0) continue;
			img.setRGB(x, y, argb(spots));
			if (x + 1 < 16 && (img.getRGB(x + 1, y) >>> 24) != 0) img.setRGB(x + 1, y, argb(shadeColor(spots, 0.8)));
		}
		outline(img, darken(base, 0.4));
		return img;
	}

	// ================================================================ ARMOR LAYERS (64x32)
	// ================================================================ UPDATE 2 ITEMS
	static final int[] ICE_P = {0x4a7ab0, 0x6a9ad0, 0x8fbde8, 0xb8dcf8, 0xdff2ff, 0xffffff};
	static final int[] BONE_P = {0x6e6450, 0x8f8468, 0xb0a585, 0xcfc6a6, 0xebe4cc, 0xffffff};
	static final int[] SOUL_P = {0x0a3a3a, 0x146060, 0x1e8a8a, 0x3ab8b8, 0x7ae8e8, 0xd0ffff};
	static final int[] STORM_P = {0x1c2a40, 0x304874, 0x4a6ea8, 0x7ea0d8, 0xc0d8ff, 0xfff4b8};
	static final int[] LEATHER_P = {0x3b2412, 0x5a3a1e, 0x7a522c, 0x9a6a3c, 0xb8844e, 0xd8a868};
	static final int[] IRON_P = {0x3a3a44, 0x5a5a66, 0x7a7a88, 0x9a9aaa, 0xc4c4d0, 0xffffff};
	static final int[] OBSIDIAN_P = {0x0c0814, 0x22182e, 0x3a2a52, 0x5a3a8a, 0x8a5ad0, 0xd0a0ff};
	static final int[] MAGMA_P = {0x2a1010, 0x4a1a10, 0x8a2a10, 0xd0501a, 0xffa040, 0xffe080};

	static void items2() throws IOException {
		// altars
		save(altar(new int[]{0x16121a, 0x221c28, 0x302838, 0x40364a, 0x584a64, 0x6a5a78}, GOLD, MAGMA_P, 61, true), "block/colossus_altar_top");
		save(altar(new int[]{0x16121a, 0x221c28, 0x302838, 0x40364a, 0x584a64, 0x6a5a78}, GOLD, MAGMA_P, 62, false), "block/colossus_altar_side");
		save(altar(new int[]{0xb8b080, 0xc8c090, 0xd8d0a0, 0xe4dcb0, 0xf0e8c0, 0xfff8d8}, OBSIDIAN_P, VOIDSHARD, 63, true), "block/herald_altar_top");
		save(altar(new int[]{0xb8b080, 0xc8c090, 0xd8d0a0, 0xe4dcb0, 0xf0e8c0, 0xfff8d8}, OBSIDIAN_P, VOIDSHARD, 64, false), "block/herald_altar_side");
		save(altar(ICE_P, IRON_P, ICE_P, 65, true), "block/lich_altar_top");
		save(altar(ICE_P, IRON_P, ICE_P, 66, false), "block/lich_altar_side");
		save(altar(SKYSTONE, STARMETAL, STORM_P, 67, true), "block/drake_altar_top");
		save(altar(SKYSTONE, STARMETAL, STORM_P, 68, false), "block/drake_altar_side");
		save(altar(UMBRAL_STONE, BONE_P, SOUL_P, 69, true), "block/king_altar_top");
		save(altar(UMBRAL_STONE, BONE_P, SOUL_P, 70, false), "block/king_altar_side");

		// boss drops and sigils
		save(core(MAGMA_P, GOLD), "item/magma_core");
		save(orb(OBSIDIAN_P, VOIDSHARD, 201), "item/herald_eye");
		save(heart(ICE_P), "item/frost_heart");
		save(feather(STORM_P), "item/storm_feather");
		save(crown(GOLD, SOUL_P), "item/hollow_crown");
		save(sigil(MAGMA_P, GOLD), "item/colossus_sigil");
		save(sigil(OBSIDIAN_P, VOIDSHARD), "item/herald_sigil");
		save(sigil(IRON_P, ICE_P), "item/lich_sigil");
		save(sigil(STARMETAL, STORM_P), "item/drake_sigil");
		save(sigil(BONE_P, SOUL_P), "item/king_sigil");

		// weapons
		save(hammer(MAGMA_P, GOLD, SHADOW_STEEL), "item/infernal_maul");
		save(sword(OBSIDIAN_P, VOIDSHARD, SHADOW_STEEL, true), "item/void_reaver");
		save(sword(ICE_P, IRON_P, FROST_HANDLE, true), "item/frostbite_blade");
		save(spear(STORM_P, GOLD, WOOD), "item/thunder_spear");
		save(staff(BONE_P, SOUL_P), "item/bone_scepter");
		save(spear(GLOWCRYSTAL, GOLD, AURORA_WOOD), "item/sky_pike");
		save(staff(SHADOW_STEEL, MAGMA_P), "item/meteor_staff");
		for (int pull = 0; pull <= 3; pull++) {
			save(bow(STORM_P, GOLD, pull), pull == 0 ? "item/storm_bow" : "item/storm_bow_pulling_" + (pull - 1));
		}

		// gear
		save(fan(STORM_P, AURORA_WOOD), "item/gale_fan");
		save(hourglass(GOLD, GLOWCRYSTAL), "item/chrono_hourglass");
		save(amulet(GOLD, AURORA_LEAF), "item/aurora_charm");
		save(hook(IRON_P, WOOD), "item/grappling_hook");
		save(feather(MAGMA_P), "item/phoenix_feather");
		save(orb(VOIDSHARD, OBSIDIAN_P, 202), "item/void_pearl");
		save(shard(new int[]{0x2a6a8a, 0x3a9ac0, 0x5ac8e8, 0x9ae8ff, 0xd8faff, 0xffffff}, 203), "item/warp_crystal");
		save(magnet(), "item/magnet_charm");
		save(backpack(LEATHER_P, GOLD), "item/backpack");
		save(pickaxe(OBSIDIAN_P, SHADOW_STEEL), "item/excavator_pickaxe");
		save(axe(GLOWCRYSTAL, AURORA_WOOD), "item/lumber_axe");

		// starmetal armor
		save(armor(HELMET, STARMETAL, GOLD), "item/starmetal_helmet");
		save(armor(CHEST, STARMETAL, GOLD), "item/starmetal_chestplate");
		save(armor(LEGS, STARMETAL, GOLD), "item/starmetal_leggings");
		save(armor(BOOTS, STARMETAL, GOLD), "item/starmetal_boots");

		// food
		save(berries(), "item/lumen_berries");
		save(fruit(AURORA_LEAF), "item/aurora_fruit");
		save(stew(SHADECAP, GLOW_SPOT), "item/shadecap_stew");
		save(pie(), "item/starfruit_pie");
		save(pepper(), "item/ember_pepper");

		// eggs
		save(egg(0x4a1a10, 0xffa040), "item/infernal_colossus_spawn_egg");
		save(egg(0x22182e, 0xd08bff), "item/void_herald_spawn_egg");
		save(egg(0x6a9ad0, 0xffffff), "item/frost_lich_spawn_egg");
		save(egg(0x304874, 0xffe066), "item/tempest_drake_spawn_egg");
		save(egg(0x948c72, 0x3ab8b8), "item/hollow_king_spawn_egg");
	}

	static final int[] FROST_HANDLE = {0x1a3a6a, 0x24508a, 0x3068a8, 0x4282c4, 0x5a9ade, 0x8ac0f0};

	static BufferedImage spear(int[] head, int[] trim, int[] shaft) {
		char[][] g = grid();
		for (int t = 0; t < 12; t++) set(g, 2 + t, 13 - t, t % 2 == 0 ? 'H' : 'h');
		// leaf-shaped tip
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double u = ((x - 12.5) - (y - 2.5)) / Math.sqrt(2), v = ((x - 12.5) + (y - 2.5)) / Math.sqrt(2);
			if (Math.abs(v) < 2.6 && Math.abs(u) < 1.6 - Math.abs(v) * 0.35) set(g, x, y, Math.abs(v) < 1 ? '5' : 'x');
		}
		set(g, 10, 5, 'g'); set(g, 11, 6, 'g'); set(g, 9, 4, 'g');
		BufferedImage img = shadeMask(g, head, shaft, trim);
		glowEdge(img, head[4]);
		return img;
	}

	static BufferedImage crown(int[] metal, int[] gem) {
		char[][] g = parse(
				"................",
				"................",
				"................",
				"..x...x..x...x..",
				"..xx..xx.xx.xx..",
				"..xxx.xxxxx.xx..",
				"..xxxxxxxxxxxx..",
				"..xxgxxxgxxxgx..",
				"..xxxxxxxxxxxx..",
				"..HHHHHHHHHHHH..",
				"................");
		return shadeMask(g, metal, metal, gem);
	}

	static BufferedImage fan(int[] blade, int[] handle) {
		char[][] g = grid();
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x - 3.5, dy = 12.5 - y;
			double r = Math.hypot(dx, dy), ang = Math.toDegrees(Math.atan2(dy, dx));
			if (r < 11.5 && r > 3 && ang > 5 && ang < 85) set(g, x, y, ((int) (ang / 10)) % 2 == 0 ? 'x' : 'X');
		}
		for (int i = 0; i < 4; i++) set(g, 3 - i / 2, 12 + i / 2, 'h');
		return shadeMask(g, blade, handle, GOLD);
	}

	static BufferedImage hourglass(int[] frame, int[] sand) {
		char[][] g = parse(
				"................",
				"...HHHHHHHHHH...",
				"....h......h....",
				"....hgggggGh....",
				"....h.gggg.h....",
				"....h..gg..h....",
				"....h..gg..h....",
				"....h..gG..h....",
				"....h..gg..h....",
				"....h.gggg.h....",
				"....hggggggh....",
				"....hGgggggh....",
				"...HHHHHHHHHH...",
				"................");
		BufferedImage img = shadeMask(g, sand, frame, sand);
		glowEdge(img, sand[4]);
		return img;
	}

	static BufferedImage amulet(int[] chain, int[] gem) {
		BufferedImage img = img(16, 16);
		for (int i = 0; i <= 20; i++) {
			double a = Math.PI * i / 20;
			int x = (int) Math.round(7.5 + Math.cos(a) * 5.5), y = (int) Math.round(6 - Math.sin(a) * 5);
			img.setRGB(x, y, argb(chain[(i % 2) + 2]));
		}
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.abs(x - 7.5) + Math.abs(y - 10.5);
			if (d < 3.5) img.setRGB(x, y, argb(d < 1.2 ? gem[5] : d < 2.4 ? gem[3] : gem[1]));
		}
		outline(img, darken(chain[0], 0.5));
		return img;
	}

	static BufferedImage hook(int[] metal, int[] rope) {
		char[][] g = grid();
		for (int t = 0; t < 8; t++) set(g, 2 + t, 13 - t, 'h');
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - 11.5, y - 4.5);
			if (Math.abs(d - 3) < 0.9 && !(x < 11 && y > 4)) set(g, x, y, 'x');
		}
		set(g, 9, 6, 'x'); set(g, 10, 5, 'X');
		return shadeMask(g, metal, rope, metal);
	}

	static BufferedImage magnet() {
		char[][] g = parse(
				"................",
				"................",
				"...gg......gg...",
				"...gg......gg...",
				"...xx......xx...",
				"...xx......xx...",
				"...xx......xx...",
				"...xx......xx...",
				"...xxx....xxx...",
				"....xxx..xxx....",
				".....xxxxxx.....",
				"......xxxx......",
				"................");
		return shadeMask(g, new int[]{0x6a1010, 0x9a1a1a, 0xc82828, 0xe84a4a, 0xff8080, 0xffffff}, IRON_P, IRON_P);
	}

	static BufferedImage backpack(int[] leather, int[] buckle) {
		char[][] g = parse(
				"................",
				"......hhhh......",
				".....h....h.....",
				"...xxxxxxxxxx...",
				"..xxxxxxxxxxxx..",
				"..xxxxxggxxxxx..",
				"..xxxxxggxxxxx..",
				"..xXXXXXXXXXXx..",
				"..xxxxxxxxxxxx..",
				"..xxxxxxxxxxxx..",
				"..xxxxxxxxxxxx..",
				"..xxxxxxxxxxxx..",
				"...xxxxxxxxxx...",
				"................");
		return shadeMask(g, leather, LEATHER_P, buckle);
	}

	static BufferedImage berries() {
		BufferedImage img = img(16, 16);
		int[][] b = {{5, 8}, {9, 7}, {7, 11}, {11, 11}, {4, 12}};
		for (int[] p : b) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
			double d = Math.hypot(dx, dy);
			if (d < 2.1) img.setRGB(p[0] + dx, p[1] + dy, argb(d < 0.8 ? 0xffffff : dx + dy < 0 ? 0x7ff5da : 0x2bbfa9));
		}
		for (int y = 2; y < 6; y++) img.setRGB(8, y, argb(0x3b6a2a));
		img.setRGB(9, 3, argb(0x5a9a3a)); img.setRGB(10, 3, argb(0x5a9a3a));
		outline(img, 0x0b2a2e);
		return img;
	}

	static BufferedImage fruit(int[] pal) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot((x - 7.5) / 1.05, y - 9);
			if (d < 5.2) {
				double l = 1 - Math.hypot(x - 5.5, y - 7) / 9.0;
				img.setRGB(x, y, argb(pal[clamp((int) (l * 6), 0, 5)]));
			}
		}
		img.setRGB(8, 3, argb(0x3b2614)); img.setRGB(8, 2, argb(0x3b2614)); img.setRGB(9, 2, argb(0x2bbfa9)); img.setRGB(10, 2, argb(0x46dcc0));
		outline(img, darken(pal[0], 0.5));
		return img;
	}

	static BufferedImage stew(int[] cap, int[] glow) {
		char[][] g = parse(
				"................",
				"................",
				"................",
				"................",
				"................",
				"................",
				"..gggggggggggg..",
				"..xggxggxgggxx..",
				"..xxxxxxxxxxxx..",
				"...xxxxxxxxxx...",
				"....xxxxxxxx....",
				".....hhhhhh.....",
				"................");
		return shadeMask(g, new int[]{0x4a3020, 0x6a4a2e, 0x8a6440, 0xa87e52, 0xc89a68, 0xe0b888}, WOOD, new int[]{cap[2], cap[3], glow[0], glow[1], glow[2], glow[3]});
	}

	static BufferedImage pie() {
		char[][] g = parse(
				"................",
				"................",
				"................",
				"................",
				"................",
				"......xxxx......",
				"...xxxgxxgxxx...",
				"..xxgxxxxxxgxx..",
				"..xxxxxgGxxxxx..",
				"..hhhhhhhhhhhh..",
				"..HHHHHHHHHHHH..",
				"...hhhhhhhhhh...",
				"................");
		return shadeMask(g, new int[]{0x8a5a2a, 0xb07a3a, 0xd09a4a, 0xe8b860, 0xf8d888, 0xfff0c0}, WOOD, new int[]{0x8a6a12, 0xd1a21f, 0xffe066, 0xfff4b8, 0xffffff, 0xffffff});
	}

	static BufferedImage pepper() {
		char[][] g = grid();
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double t = (x + y) / 30.0;
			double cx = 3 + t * 9, cy = 12 - t * 9;
			double d = Math.hypot(x - (5 + (12 - y) * 0.55), y - (y));
			if (x + y > 8 && x + y < 22 && Math.abs((x - y) - (-1 + (x + y - 15) * 0.15)) < 2.8 - Math.abs(x + y - 15) * 0.12) set(g, x, y, 'x');
		}
		set(g, 12, 3, 'h'); set(g, 13, 2, 'h'); set(g, 12, 2, 'h');
		BufferedImage img = shadeMask(g, MAGMA_P, new int[]{0x2a4a1a, 0x3b6a2a, 0x4a8a3a, 0x5aa04a, 0x70c060, 0x90e080}, MAGMA_P);
		return img;
	}

	// ================================================================ UPDATE 3 BLOCKS + ITEMS
	static final int[] ECHO_BARK = {0x041418, 0x08262c, 0x0d3a42, 0x135058, 0x1a6a72};
	static final int[] ECHO_WOOD = {0x0e4a4e, 0x146064, 0x1a787c, 0x229094, 0x2aa8ac};
	static final int[] ECHO_LEAF = {0x0a3a40, 0x105058, 0x1a7078, 0x29a8b0, 0x5cf2ff, 0xd0ffff};
	static final int[] ECHO_CRYSTAL = {0x0a3a4a, 0x146a80, 0x29a8c0, 0x5cdcf0, 0xb0f8ff, 0xffffff};
	static final int[] DEEPSLATE = {0x2a2a30, 0x34343c, 0x404048, 0x4c4c56, 0x5a5a64, 0x6a6a74};

	static void items3() throws IOException {
		save(logSide(ECHO_BARK, 81), "block/echo_log");
		save(logTop(ECHO_BARK, ECHO_WOOD, 82), "block/echo_log_top");
		save(planks(ECHO_WOOD, 83), "block/echo_planks");
		save(leaves(ECHO_LEAF, 84), "block/echo_leaves");
		save(flower(new int[]{0x0a3a40, 0x105058, 0x1a7078, 0x29a8b0}, ECHO_CRYSTAL, 85), "block/echo_bloom");
		BufferedImage deep = stone(DEEPSLATE, 86, 0.05);
		save(ore(deep, ECHO_CRYSTAL, 87), "block/echo_crystal_ore");
		save(crystal(ECHO_CRYSTAL, 88), "block/echo_crystal_block");
		save(keyhole(false), "block/sculk_keyhole");
		save(keyhole(true), "block/sculk_keyhole_filled");
		saveAnimated(portal(new int[]{0x02181c, 0x07383e, 0x0d6a72, 0x29dfeb, 0xc0ffff}, 89, 32), "block/sculk_portal", 2);

		save(key(SHADOW_STEEL, ECHO_CRYSTAL), "item/sculk_key");
		save(shard(ECHO_CRYSTAL, 301), "item/echo_crystal");
		save(heart(new int[]{0x0d3a42, 0x135058, 0x1a6a72, 0x29dfeb, 0x5cf2ff, 0xb0ffff}), "item/echo_heart");
		save(sword(ECHO_CRYSTAL, SHADOW_STEEL, SHADOW_STEEL, true), "item/sonic_blade");
		save(horn(), "item/echo_horn");
		save(meat(new int[]{0x8a3a4a, 0xb04a5a, 0xd06a7a, 0xe890a0, 0xf8c0c8, 0xffe0e8}), "item/glimmer_venison");
		save(meat(new int[]{0x5a2a1a, 0x7a3a22, 0x9a522e, 0xb86a3e, 0xd08a58, 0xe8b080}), "item/cooked_glimmer_venison");
		save(antler(), "item/glimmer_antler");
		save(fluff(), "item/cloud_fluff");
		save(bottle(new int[]{0xc8d8ec, 0xe8f2fa, 0xffffff}), "item/cloud_bottle");
		save(meat(new int[]{0x1a504c, 0x22665e, 0x2a8072, 0x3a9a86, 0x5ab8a0, 0x8ad8c0}), "item/toad_leg");
		save(blob(new int[]{0x8a6a10, 0xc8a020, 0xffd060, 0xfff0a0, 0xffffff, 0xffffff}), "item/glow_jelly");
		save(shellItem(), "item/snail_shell");
		save(blob(new int[]{0x0a3a40, 0x105058, 0x1a7078, 0x29a8b0, 0x5cf2ff, 0xd0ffff}), "item/sculk_slime");
		save(shard(EMBER, 302), "item/salamander_scale");
		save(amulet(GOLD, EMBER), "item/salamander_charm");
		save(blob(new int[]{0x3a1a5a, 0x5a2a8a, 0x8a4ac0, 0xb07ae0, 0xd8b0ff, 0xffffff}), "item/void_jelly");
		save(egg(0x0d3a42, 0x5cf2ff), "item/echo_warden_spawn_egg");
		save(egg(0x24383a, 0x29dfeb), "item/sculk_stalker_spawn_egg");
		save(egg(0x9886be, 0xffffff), "item/glimmer_deer_spawn_egg");
		save(egg(0xe8f2fa, 0xffc8dc), "item/cloud_bunny_spawn_egg");
		save(egg(0x1a504c, 0xffe066), "item/shade_toad_spawn_egg");
		save(egg(0x5a4018, 0xfff0a0), "item/lantern_bug_spawn_egg");
		save(egg(0x135058, 0x5cf2ff), "item/sculk_snail_spawn_egg");
		save(egg(0xa83c10, 0xffb84d), "item/ember_salamander_spawn_egg");
		save(egg(0x5a2a8a, 0xff7aff), "item/void_jelly_spawn_egg");
		save(egg(0x3a86b8, 0xf2c53d), "item/realm_trader_spawn_egg");
		save(ventTop(), "block/cloud_vent_top");
		save(ventSide(), "block/cloud_vent_side");
	}

	/** Cloud vent top: skystone rim with a glowing grate and swirling cloud in the middle. */
	static BufferedImage ventTop() {
		BufferedImage img = stone(SKYSTONE, 91, 0.03);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - 7.5, y - 7.5);
			if (x == 0 || y == 0 || x == 15 || y == 15) img.setRGB(x, y, argb(SKYSTONE[1]));
			else if (d < 6.2) {
				double a = Math.atan2(y - 7.5, x - 7.5) + d * 0.6;
				boolean swirl = Math.sin(a * 3) > 0.2;
				img.setRGB(x, y, argb(d < 1.6 ? 0xffffff : swirl ? 0xe8f6ff : 0x9fdcff));
				if ((x + y) % 4 == 0 && d > 2.5) img.setRGB(x, y, argb(GLOWCRYSTAL[3]));
			} else if (d < 7.2) img.setRGB(x, y, argb(GLOWCRYSTAL[2]));
		}
		return img;
	}

	static BufferedImage ventSide() {
		BufferedImage img = bricks(SKYSTONE, 92);
		for (int x = 0; x < 16; x++) {
			img.setRGB(x, 0, argb(GLOWCRYSTAL[3]));
			img.setRGB(x, 1, argb(GLOWCRYSTAL[1]));
			if (x % 4 == 1) for (int y = 3; y < 13; y++) img.setRGB(x, y, argb(y % 3 == 0 ? 0xffffff : 0xc8ecff));
		}
		return img;
	}

	static BufferedImage keyhole(boolean filled) {
		BufferedImage img = stone(DEEPSLATE, 90, 0.04);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			boolean border = x == 0 || y == 0 || x == 15 || y == 15;
			if (border) img.setRGB(x, y, argb(0x1a1a20));
			else if (x == 1 || y == 1) img.setRGB(x, y, argb(0x6a6a74));
			double d = Math.hypot(x - 7.5, y - 6);
			boolean hole = d < 2.2 || (Math.abs(x - 7.5) < 1.1 && y >= 6 && y <= 12);
			if (hole) img.setRGB(x, y, argb(filled ? (d < 1.2 ? 0xb0ffff : 0x29dfeb) : 0x05080a));
			else if (d < 3.4 || (Math.abs(x - 7.5) < 2.2 && y >= 6 && y <= 13)) img.setRGB(x, y, argb(filled ? 0x0d6a72 : 0x135058));
		}
		return img;
	}

	static BufferedImage horn() {
		char[][] g = grid();
		for (int i = 0; i < 12; i++) {
			int x = 2 + i, y = 12 - (int) Math.round(Math.sin(i / 11.0 * Math.PI * 0.7) * 6);
			int w = 1 + i / 4;
			for (int k = 0; k < w; k++) set(g, x, y + k, i > 9 ? 'g' : 'x');
		}
		return shadeMask(g, BONE, BONE, ECHO_CRYSTAL);
	}

	static BufferedImage meat(int[] pal) {
		char[][] g = parse(
				"................",
				"................",
				"................",
				"........xxxx....",
				"......xxxxxxx...",
				".....xxxxxxxxx..",
				"....xxxxxxxxxx..",
				"...xxxxxxxxxx...",
				"..hhxxxxxxxx....",
				".hHh.xxxxxx.....",
				".hh.............",
				"................");
		return shadeMask(g, pal, BONE, pal);
	}

	static BufferedImage antler() {
		char[][] g = parse(
				"................",
				"..x.......x.....",
				"..x..x....x..x..",
				"..xx.x....xx.x..",
				"...xxx.....xxx..",
				"....xx..x...xx..",
				"....xx.xx...xx..",
				".....xxx...xx...",
				"......xx..xx....",
				"......xxxxx.....",
				".......ggg......",
				"................");
		BufferedImage img = shadeMask(g, new int[]{0xb0a080, 0xc8b898, 0xe0d0b0, 0xf0e4c8, 0xfff4e0, 0xffffff}, BONE, GLOWCRYSTAL);
		glowEdge(img, 0x9ff2ff);
		return img;
	}

	static BufferedImage fluff() {
		BufferedImage img = img(16, 16);
		int[][] puffs = {{6, 9, 4}, {10, 8, 3}, {8, 6, 3}, {4, 10, 2}};
		for (int[] p : puffs) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot(x - p[0], y - p[1]);
			if (d < p[2]) img.setRGB(x, y, argb(d < p[2] - 1.5 ? 0xffffff : (x + y) % 3 == 0 ? 0xd8e6f4 : 0xe8f2fa));
		}
		outline(img, 0x8aa0c0);
		return img;
	}

	static BufferedImage bottle(int[] cloud) {
		char[][] g = parse(
				"................",
				"......hhhh......",
				".......hh.......",
				".......xx.......",
				"......xxxx......",
				".....xggggx.....",
				"....xggggggx....",
				"....xggggggx....",
				"....xggggggx....",
				"....xggggggx....",
				".....xxxxxx.....",
				"................");
		return shadeMask(g, new int[]{0x8ab0c8, 0xa8c8dc, 0xc8e0ee, 0xe0f0f8, 0xffffff, 0xffffff}, WOOD, cloud);
	}

	static BufferedImage blob(int[] pal) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double d = Math.hypot((x - 7.5) / 1.2, y - 9);
			if (d < 4.5 + Math.sin(x * 1.3) * 0.4) {
				double l = 1 - Math.hypot(x - 5.5, y - 7) / 8.0;
				img.setRGB(x, y, argb(pal[clamp((int) (l * 6), 0, 5)]));
			}
		}
		img.setRGB(5, 7, argb(0xffffff));
		outline(img, darken(pal[0], 0.5));
		return img;
	}

	static BufferedImage shellItem() {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double dx = x - 7.5, dy = y - 8;
			double r = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
			if (r < 6) {
				double spiral = (r - (a + Math.PI) * 0.8) % 2.4;
				boolean line = Math.abs(spiral) < 0.6;
				img.setRGB(x, y, argb(line ? 0x5cf2ff : r < 3 ? 0x1a6a72 : 0x135058));
			}
		}
		outline(img, 0x041418);
		return img;
	}

	static void equipment() throws IOException {
		save(armorLayer(STARMETAL, GOLD, false, 205), "entity/equipment/humanoid/starmetal");
		save(armorLayer(STARMETAL, GOLD, true, 206), "entity/equipment/humanoid_leggings/starmetal");
		save(armorLayer(GLOWCRYSTAL, GOLD, false, 201), "entity/equipment/humanoid/glowcrystal");
		save(armorLayer(GLOWCRYSTAL, GOLD, true, 202), "entity/equipment/humanoid_leggings/glowcrystal");
		save(armorLayer(VOIDSHARD, SHADOW_STEEL, false, 203), "entity/equipment/humanoid/voidshard");
		save(armorLayer(VOIDSHARD, SHADOW_STEEL, true, 204), "entity/equipment/humanoid_leggings/voidshard");
	}

	static BufferedImage armorLayer(int[] mat, int[] trim, boolean legs, long seed) {
		BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
		double[][] n = fbm(64, 32, seed, new int[]{8, 4, 2}, new double[]{0.5, 0.3, 0.2});
		// UV boxes of the vanilla humanoid model: {u, v, w, h, d}
		int[][] boxes = legs
				? new int[][]{{16, 16, 8, 12, 4}, {0, 16, 4, 12, 4}}
				: new int[][]{{0, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {40, 16, 4, 12, 4}, {0, 16, 4, 12, 4}};
		for (int[] b : boxes) {
			paintBox(img, b[0], b[1], b[2], b[3], b[4], (x, y, face, fx, fy, fw, fh) -> {
				boolean edge = fx == 0 || fy == 0 || fx == fw - 1 || fy == fh - 1;
				double v = n[x % 64][y % 32];
				int c;
				if (edge) c = trim[fx == 0 || fy == 0 ? 3 : 1];
				else c = mat[clamp(1 + (int) (v * 4), 1, 4)];
				// gem in the middle of front faces
				if (face == 1 && Math.abs(fx - fw / 2.0 + 0.5) < 1 && Math.abs(fy - fh / 2.0 + 0.5) < 1) c = mat[5];
				return c;
			});
		}
		// helmet visor
		if (!legs) for (int x = 9; x < 15; x++) img.setRGB(x, 12, argb(darken(mat[0], 0.4)));
		return img;
	}

	interface FacePainter {
		int paint(int x, int y, int face, int fx, int fy, int fw, int fh);
	}

	/** Paint a model cube's standard UV layout. face: 0 top,1 front,2 right,3 back,4 left,5 bottom */
	static void paintBox(BufferedImage img, int u, int v, int w, int h, int d, FacePainter p) {
		int[][] faces = {
				{u + d, v, w, d, 0},          // top
				{u + d + w, v, w, d, 5},      // bottom
				{u, v + d, d, h, 2},          // right side (model's)
				{u + d, v + d, w, h, 1},      // front
				{u + d + w, v + d, d, h, 4},  // left side
				{u + d + w + d, v + d, w, h, 3} // back
		};
		for (int[] f : faces) {
			for (int fy = 0; fy < f[3]; fy++) for (int fx = 0; fx < f[2]; fx++) {
				int x = f[0] + fx, y = f[1] + fy;
				if (x >= img.getWidth() || y >= img.getHeight()) continue;
				img.setRGB(x, y, argb(p.paint(x, y, f[4], fx, fy, f[2], f[3])));
			}
		}
	}

	// ================================================================ GUI: logo, panorama, icon
	static void gui() throws IOException {
		BufferedImage logo = logo();
		save(logo, "gui/title/glowcube_logo");
		BufferedImage icon = img(128, 128);
		Graphics2D g = icon.createGraphics();
		BufferedImage block = crystal(GLOWCRYSTAL, 18);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(block, 0, 0, 128, 128, null);
		g.dispose();
		File f = new File("src/main/resources/assets/glowcube_realms/icon.png");
		f.getParentFile().mkdirs();
		ImageIO.write(icon, "png", f);
		panorama();
	}

	static BufferedImage logo() {
		int sw = 256, sh = 64;
		BufferedImage small = img(sw, sh);
		Graphics2D g = small.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		Font font = new Font("Arial Black", Font.BOLD, 34);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		String text = "GLOWCUBE";
		int tw = fm.stringWidth(text);
		int tx = (sw - tw) / 2, ty = 36;
		g.setColor(Color.WHITE);
		g.drawString(text, tx, ty);
		Font sub = new Font("Arial Black", Font.BOLD, 13);
		g.setFont(sub);
		String s2 = "R E A L M S";
		int sw2 = g.getFontMetrics().stringWidth(s2);
		g.drawString(s2, (sw - sw2) / 2, 55);
		g.dispose();
		// mask -> colored, extruded, outlined pixel art
		boolean[][] mask = new boolean[sw][sh];
		for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) mask[x][y] = (small.getRGB(x, y) >>> 24) > 100;
		BufferedImage art = img(sw, sh);
		int depth = 4;
		for (int k = depth; k >= 1; k--) {
			for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
				int sx = x - k, sy = y - k;
				if (sx >= 0 && sy >= 0 && mask[sx][sy]) art.setRGB(x, y, argb(shadeColor(0x2a0a4a, 0.6 + k * 0.08)));
			}
		}
		for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
			if (!mask[x][y]) continue;
			boolean subtitle = y > 40;
			int c;
			if (subtitle) {
				double t = (y - 44) / 12.0;
				c = lerp(GOLD[5], GOLD[2], clamp01(t));
			} else {
				double t = (y - 10) / 28.0;
				c = t < 0.5 ? lerp(0xe6fbff, 0x5cdfff, t * 2) : lerp(0x5cdfff, 0xb36bff, (t - 0.5) * 2);
				// crystal facets
				if (((x + y * 2) % 9) == 0) c = lerp(c, 0xffffff, 0.45);
			}
			// top-edge highlight
			if (y > 0 && !mask[x][y - 1]) c = lerp(c, 0xffffff, 0.6);
			art.setRGB(x, y, argb(c));
		}
		outline(art, 0x0b0414);
		// upscale ×4 and add soft glow
		int W = sw * 4, H = sh * 4;
		BufferedImage out = img(W, H);
		BufferedImage glow = img(W, H);
		Graphics2D gg = glow.createGraphics();
		for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
			if (mask[x][y] && y < 40) {
				gg.setColor(new Color(0x5c, 0xdf, 0xff, 26));
				gg.fill(new Ellipse2D.Double(x * 4 - 14, y * 4 - 14, 32, 32));
			}
		}
		gg.dispose();
		Graphics2D go = out.createGraphics();
		go.drawImage(glow, 0, 0, null);
		go.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		go.drawImage(art, 0, 0, W, H, null);
		go.dispose();
		return out;
	}

	/** Seamless cubemap: night sky with aurora and floating island silhouettes. */
	static void panorama() throws IOException {
		int S = 512;
		Random r = new Random(77);
		double[][] stars = new double[2600][4];
		for (double[] s : stars) {
			double z = r.nextDouble() * 2 - 1, a = r.nextDouble() * Math.PI * 2;
			double rr = Math.sqrt(1 - z * z);
			s[0] = rr * Math.cos(a); s[1] = z; s[2] = rr * Math.sin(a); s[3] = r.nextDouble();
		}
		double[] islandAz = new double[9], islandEl = new double[9], islandW = new double[9];
		for (int i = 0; i < islandAz.length; i++) {
			islandAz[i] = i * (Math.PI * 2 / islandAz.length) + r.nextDouble() * 0.4;
			islandEl[i] = -0.05 + r.nextDouble() * 0.28;
			islandW[i] = 0.12 + r.nextDouble() * 0.16;
		}
		for (int face = 0; face < 6; face++) {
			BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_RGB);
			for (int py = 0; py < S; py++) for (int px = 0; px < S; px++) {
				double a = (px + 0.5) / S * 2 - 1, b = (py + 0.5) / S * 2 - 1;
				double[] d = faceDir(face, a, b);
				img.setRGB(px, py, sky(d, stars, islandAz, islandEl, islandW));
			}
			File f = new File(MC_ROOT + "gui/title/background/panorama_" + face + ".png");
			f.getParentFile().mkdirs();
			ImageIO.write(img, "png", f);
		}
	}

	/**
	 * Direction for a pixel of cubemap face. Matches vanilla CubeMap ordering:
	 * 0 = north(-Z) front, 1 = east(+X) right, 2 = south(+Z) back, 3 = west(-X) left, 4 = up, 5 = down.
	 * a: -1 left .. 1 right, b: -1 top .. 1 bottom.
	 */
	static double[] faceDir(int face, double a, double b) {
		double x, y, z;
		switch (face) {
			case 0 -> { x = a; y = -b; z = -1; }
			case 1 -> { x = 1; y = -b; z = a; }
			case 2 -> { x = -a; y = -b; z = 1; }
			case 3 -> { x = -1; y = -b; z = -a; }
			case 4 -> { x = a; y = 1; z = -b; }
			default -> { x = a; y = -1; z = b; }
		}
		double l = Math.sqrt(x * x + y * y + z * z);
		return new double[]{x / l, y / l, z / l};
	}

	static int sky(double[] d, double[][] stars, double[] iAz, double[] iEl, double[] iW) {
		double el = Math.asin(d[1]);
		double az = Math.atan2(d[2], d[0]);
		double t = clamp01((d[1] + 0.25) / 1.25);
		int col = lerp(0x2a6b8f, 0x0a0620, Math.pow(t, 0.6));
		col = lerp(col, 0x3b1d66, Math.max(0, 0.35 - Math.abs(d[1] - 0.15)) * 1.4);
		// aurora ribbons
		double ribbon = Math.sin(az * 3 + Math.sin(az * 7) * 0.6) * 0.12 + 0.42;
		double ad = Math.abs(el - ribbon);
		if (ad < 0.22 && d[1] > 0) {
			double s = Math.pow(1 - ad / 0.22, 2) * (0.55 + 0.45 * Math.sin(az * 23 + el * 9));
			col = add(col, lerp(0x2bffa0, 0xc36bff, clamp01((el - ribbon + 0.12) / 0.4)), s * 0.6);
		}
		// stars
		if (d[1] > -0.02) for (double[] s : stars) {
			double dot = s[0] * d[0] + s[1] * d[1] + s[2] * d[2];
			double size = s[3] > 0.97 ? 0.99996 : 0.999993;
			if (dot > size) {
				double fade = clamp01((d[1] + 0.02) * 4);
				col = add(col, s[3] > 0.6 ? 0xffffff : 0xbfe8ff, (0.35 + s[3] * 0.65) * fade);
				break;
			}
		}
		// floating islands
		for (int i = 0; i < iAz.length; i++) {
			double daz = Math.atan2(Math.sin(az - iAz[i]), Math.cos(az - iAz[i]));
			double u = daz / iW[i];
			if (Math.abs(u) > 1) continue;
			double top = iEl[i] + 0.02 * Math.sin(u * 9 + i);
			double bottom = top - (1 - u * u) * iW[i] * 0.9 - 0.01 * Math.sin(u * 23);
			if (el < top && el > bottom) {
				double depth = (top - el) / Math.max(0.001, top - bottom);
				int rock = lerp(0x1a1030, 0x07040f, depth);
				if (top - el < 0.008) rock = 0x46dcc0;
				else if (top - el < 0.016) rock = 0x1d6a6a;
				col = rock;
				// glowing crystals hanging below
				if (depth > 0.85 && ((int) (u * 40) % 7 == 0)) col = 0x6fdcff;
			}
			// tree silhouettes on top
			if (el >= top && el < top + 0.05 * (1 - u * u)) {
				double tree = Math.sin(u * 31 + i * 3);
				if (tree > 0.55 && el < top + (tree - 0.55) * 0.12) col = 0x2a1340;
			}
		}
		// ground haze at the bottom
		if (d[1] < -0.1) col = lerp(col, 0x05030a, clamp01((-d[1] - 0.1) * 2));
		return col;
	}

	// ================================================================ helpers
	static BufferedImage img(int w, int h) {
		return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	}

	static BufferedImage copy(BufferedImage src) {
		BufferedImage c = img(src.getWidth(), src.getHeight());
		c.getGraphics().drawImage(src, 0, 0, null);
		return c;
	}

	static void save(BufferedImage img, String path) throws IOException {
		File f = new File(ROOT + path + ".png");
		f.getParentFile().mkdirs();
		ImageIO.write(img, "png", f);
	}

	static void saveAnimated(BufferedImage[] frames, String path, int frametime) throws IOException {
		int w = frames[0].getWidth(), h = frames[0].getHeight();
		BufferedImage strip = img(w, h * frames.length);
		for (int i = 0; i < frames.length; i++) strip.getGraphics().drawImage(frames[i], 0, i * h, null);
		save(strip, path);
		Files.writeString(new File(ROOT + path + ".png.mcmeta").toPath(),
				"{\n  \"animation\": {\n    \"frametime\": " + frametime + "\n  }\n}\n");
	}

	static void outline(BufferedImage img, int color) {
		int w = img.getWidth(), h = img.getHeight();
		List<int[]> pts = new ArrayList<>();
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			if ((img.getRGB(x, y) >>> 24) != 0) continue;
			boolean near = false;
			for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nx = x + o[0], ny = y + o[1];
				if (nx >= 0 && ny >= 0 && nx < w && ny < h && (img.getRGB(nx, ny) >>> 24) > 200) near = true;
			}
			if (near) pts.add(new int[]{x, y});
		}
		for (int[] p : pts) img.setRGB(p[0], p[1], argb(color));
	}

	/** Brightens pixels that touch the outline on the upper-left: a rim-light effect. */
	static void glowEdge(BufferedImage img, int color) {
		int w = img.getWidth(), h = img.getHeight();
		BufferedImage src = copy(img);
		for (int y = 1; y < h; y++) for (int x = 1; x < w; x++) {
			int c = src.getRGB(x, y);
			if ((c >>> 24) == 0) continue;
			int up = src.getRGB(x, y - 1), left = src.getRGB(x - 1, y);
			if (isDark(up) || isDark(left)) img.setRGB(x, y, argb(lerp(c & 0xFFFFFF, color, 0.5)));
		}
	}

	static boolean isDark(int c) {
		if ((c >>> 24) == 0) return false;
		int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
		return r + g + b < 90;
	}

	static double[][] fbm(int w, int h, long seed, int[] cells, double[] weights) {
		double[][] out = new double[w][h];
		Random r = new Random(seed);
		for (int o = 0; o < cells.length; o++) {
			int cell = cells[o];
			int gw = Math.max(1, w / cell), gh = Math.max(1, h / cell);
			double[][] grid = new double[gw][gh];
			for (int i = 0; i < gw; i++) for (int j = 0; j < gh; j++) grid[i][j] = r.nextDouble();
			for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
				double fx = x / (double) cell, fy = y / (double) cell;
				int x0 = (int) fx, y0 = (int) fy;
				double tx = smooth(fx - x0), ty = smooth(fy - y0);
				double a = grid[x0 % gw][y0 % gh], b = grid[(x0 + 1) % gw][y0 % gh];
				double c = grid[x0 % gw][(y0 + 1) % gh], d = grid[(x0 + 1) % gw][(y0 + 1) % gh];
				out[x][y] += weights[o] * ((a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty);
			}
		}
		return out;
	}

	static double smooth(double t) { return t * t * (3 - 2 * t); }
	static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
	static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
	static int argb(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

	static int lerp(int a, int b, double t) {
		t = clamp01(t);
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	static int add(int a, int b, double s) {
		int r = Math.min(255, (int) (((a >> 16) & 255) + ((b >> 16) & 255) * s));
		int g = Math.min(255, (int) (((a >> 8) & 255) + ((b >> 8) & 255) * s));
		int bl = Math.min(255, (int) ((a & 255) + (b & 255) * s));
		return (r << 16) | (g << 8) | bl;
	}

	static int darken(int c, double f) { return shadeColor(c, f); }

	static int shadeColor(int c, double f) {
		int r = clamp((int) (((c >> 16) & 255) * f), 0, 255);
		int g = clamp((int) (((c >> 8) & 255) * f), 0, 255);
		int b = clamp((int) ((c & 255) * f), 0, 255);
		return (r << 16) | (g << 8) | b;
	}
}
