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
		weapons3d();
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

	/** All palette colours but the brightest (kept for highlights). */
	static int[] body(int[] pal) {
		return java.util.Arrays.copyOf(pal, Math.max(2, pal.length - 1));
	}

	/** Wrapped Voronoi helper: {nearest distance, second distance, nearest index}. */
	static double[] voronoi(double[][] pts, double x, double y) {
		double best = 1e9, second = 1e9;
		int bi = 0;
		for (int i = 0; i < pts.length; i++) for (int ox = -16; ox <= 16; ox += 16) for (int oy = -16; oy <= 16; oy += 16) {
			double d = Math.hypot(x - pts[i][0] - ox, y - pts[i][1] - oy);
			if (d < best) {
				second = best;
				best = d;
				bi = i;
			} else if (d < second) second = d;
		}
		return new double[]{best, second, bi};
	}

	static double[][] points(Random r, int n) {
		double[][] p = new double[n][2];
		for (double[] q : p) {
			q[0] = r.nextDouble() * 16;
			q[1] = r.nextDouble() * 16;
		}
		return p;
	}

	/** Lumpy stone lit from the top-left, with recessed seams, carved cracks and a few glints. */
	static BufferedImage stone(int[] pal, long seed, double crackChance) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2}, new double[]{0.5, 0.3, 0.2});
		double[][] pts = points(rng, 7);
		double[][] hf = new double[16][16];
		double[][] seam = new double[16][16];
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double[] v = voronoi(pts, x + 0.5, y + 0.5);
			seam[x][y] = v[1] - v[0];
			hf[x][y] = n[x][y] * 0.55 + clamp01(seam[x][y] / 3.5) * 0.45;
		}
		int[] b = body(pal);
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double t = 0.16 + hf[x][y] * 0.6 + emboss(hf, x, y) * 1.5 + (hash(x, y, seed) - 0.5) * 0.1;
			if (seam[x][y] < 0.6) t -= 0.16;
			img.setRGB(x, y, argb(rampQ(b, t)));
		}
		// carved cracks: dark line with a lit lower edge
		for (int c = 0; c < 2 + (crackChance > 0.05 ? 1 : 0); c++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			int len = 3 + rng.nextInt(4);
			for (int s = 0; s < len; s++) {
				img.setRGB(x & 15, y & 15, argb(pal[0]));
				img.setRGB((x + 1) & 15, (y + 1) & 15, argb(lerp(img.getRGB((x + 1) & 15, (y + 1) & 15) & 0xFFFFFF, pal[pal.length - 2], 0.35)));
				if (rng.nextBoolean()) x += rng.nextBoolean() ? 1 : -1;
				else y += 1;
			}
		}
		for (int i = 0; i < 5; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[pal.length - 1]));
		return img;
	}

	/** Bricks: every brick has its own tone, a bevel (lit top/left, shaded bottom/right), chips and recessed mortar. */
	static BufferedImage bricks(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{4, 2}, new double[]{0.6, 0.4});
		double[] tone = new double[8];
		for (int i = 0; i < tone.length; i++) tone[i] = rng.nextDouble() * 0.2 - 0.1;
		int[] b = body(pal);
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int row = y / 4, ly = y % 4;
			int bx = (x + (row % 2) * 4) % 16, lx = bx % 8, brick = row * 2 + bx / 8;
			double t;
			if (ly == 3 || lx == 7) t = 0.04 + n[x][y] * 0.14;
			else {
				t = 0.5 + tone[brick] + (n[x][y] - 0.5) * 0.24;
				if (ly == 0) t += 0.2;
				else if (lx == 0) t += 0.12;
				else if (ly == 2) t -= 0.12;
				else if (lx == 6) t -= 0.08;
				if (hash(x, y, seed) > 0.93) t -= 0.22; // chip
				if (hash(x, y, seed + 1) > 0.97) t += 0.25;
			}
			img.setRGB(x, y, argb(rampQ(b, t)));
		}
		return img;
	}

	static BufferedImage chiseled(int[] pal, int[] gem, long seed) {
		BufferedImage img = stone(pal, seed, 0.02);
		int[] b = body(pal);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double base = (img.getRGB(x, y) & 0xFF) / 255.0 * 0.15;
			double t = -1;
			if (x == 0 || y == 0 || x == 15 || y == 15) t = 0.02;
			else if (x == 1 || y == 1) t = 0.85;
			else if (x == 14 || y == 14) t = 0.25;
			else if (x == 2 || y == 2) t = 0.2;          // inner recess shadow
			else if (x == 13 || y == 13) t = 0.7;
			else t = 0.5 + base + (hash(x, y, seed) - 0.5) * 0.1;
			img.setRGB(x, y, argb(rampQ(b, t)));
		}
		// carved rune: faceted diamond with an engraved ring
		for (int y = 3; y < 13; y++) for (int x = 3; x < 13; x++) {
			double dx = x - 7.5, dy = y - 7.5, d = Math.abs(dx) + Math.abs(dy);
			if (d < 1.6) img.setRGB(x, y, argb(gem[5]));
			else if (d < 2.6) img.setRGB(x, y, argb(dx + dy < 0 ? gem[4] : gem[2]));
			else if (d < 3.6) img.setRGB(x, y, argb(dx + dy < 0 ? gem[3] : gem[1]));
			else if (d < 4.3) img.setRGB(x, y, argb(rampQ(b, 0.12)));
			else if (d < 5.0) img.setRGB(x, y, argb(rampQ(b, dx + dy < 0 ? 0.3 : 0.8)));
			else if (Math.abs(d - 5.6) < 0.5 && (x + y) % 2 == 0) img.setRGB(x, y, argb(gem[1]));
		}
		return img;
	}

	static BufferedImage dirt(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{4, 2, 1}, new double[]{0.5, 0.3, 0.2});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double t = 0.12 + n[x][y] * 0.7 + emboss(n, x, y) * 1.4;
			img.setRGB(x, y, argb(rampQ(pal, t)));
		}
		// pebbles with a lit top
		for (int i = 0; i < 5; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(pal[pal.length - 1]));
			img.setRGB((x + 1) & 15, y, argb(pal[pal.length - 2]));
			img.setRGB(x, (y + 1) & 15, argb(pal[0]));
			img.setRGB((x + 1) & 15, (y + 1) & 15, argb(pal[0]));
		}
		for (int i = 0; i < 8; i++) img.setRGB(rng.nextInt(16), rng.nextInt(16), argb(pal[0]));
		return img;
	}

	static BufferedImage grassTop(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2, 1}, new double[]{0.35, 0.3, 0.2, 0.15});
		int[] b = body(pal);
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
			img.setRGB(x, y, argb(rampQ(b, 0.18 + n[x][y] * 0.62 + emboss(n, x, y) * 1.3)));
		// blades: lit tip, shaded root
		for (int i = 0; i < 26; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(pal[4]));
			img.setRGB(x, (y + 1) & 15, argb(pal[2]));
			img.setRGB((x + 1) & 15, (y + 1) & 15, argb(pal[0]));
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
			img.setRGB((x + 1) & 15, y, argb(glow[0]));
			img.setRGB(x, (y + 1) & 15, argb(lerp(glow[0], pal[1], 0.5)));
		}
		return img;
	}

	/** Grass overhang with a shaded lip, a few dripping strands and a contact shadow on the soil below. */
	static BufferedImage grassSide(BufferedImage base, int[] grass, long seed) {
		rng = new Random(seed);
		BufferedImage img = copy(base);
		for (int x = 0; x < 16; x++) {
			int depth = 3 + rng.nextInt(2) + (x % 5 == 0 ? 1 : 0);
			if (rng.nextInt(5) == 0) depth += 2; // drip
			for (int y = 0; y < depth; y++) {
				double t = 0.95 - y / (double) depth * 0.75 + (hash(x, y, seed) - 0.5) * 0.15;
				img.setRGB(x, y, argb(rampQ(grass, t)));
			}
			img.setRGB(x, depth, argb(shadeColor(img.getRGB(x, depth) & 0xFFFFFF, 0.6)));
			if (depth + 1 < 16) img.setRGB(x, depth + 1, argb(shadeColor(img.getRGB(x, depth + 1) & 0xFFFFFF, 0.82)));
		}
		return img;
	}

	/** Ore: faceted crystal clusters (lit facet, shaded facet, glint) casting a small shadow on the stone. */
	static BufferedImage ore(BufferedImage base, int[] gem, long seed) {
		rng = new Random(seed);
		BufferedImage img = copy(base);
		int[][] spots = {{3, 3}, {11, 2}, {7, 8}, {2, 12}, {12, 11}};
		boolean[][] mask = new boolean[16][16];
		for (int[] s : spots) {
			int cx = s[0] + rng.nextInt(2), cy = s[1] + rng.nextInt(2);
			int size = 1 + rng.nextInt(2);
			for (int dy = -size - 1; dy <= size + 1; dy++) for (int dx = -size; dx <= size; dx++) {
				double shape = Math.abs(dx) + Math.abs(dy) * 0.55; // upright hexagonal crystal
				if (shape > size + 0.6) continue;
				int x = cx + dx, y = cy + dy;
				if (x < 0 || y < 0 || x > 15 || y > 15) continue;
				int c;
				if (dx == 0 && dy == -1) c = gem[5];
				else if (dx <= 0 && dy < 0) c = gem[4];
				else if (dx < 0) c = gem[3];
				else if (dy < 0) c = gem[3];
				else if (dx > 0 && dy > 0) c = gem[1];
				else c = gem[2];
				img.setRGB(x, y, argb(c));
				mask[x][y] = true;
			}
		}
		// contact shadow to the lower-right of every crystal
		BufferedImage src = copy(img);
		for (int y = 0; y < 15; y++) for (int x = 0; x < 15; x++)
			if (mask[x][y] && !mask[x + 1][y + 1]) img.setRGB(x + 1, y + 1, argb(shadeColor(src.getRGB(x + 1, y + 1) & 0xFFFFFF, 0.55)));
		return img;
	}

	/** Crystal block: Voronoi facets, each with its own light angle, bright ridges and a few sparkles. */
	static BufferedImage crystal(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] p = points(rng, 7);
		double[] facing = new double[7];
		for (int i = 0; i < 7; i++) facing[i] = rng.nextDouble() * Math.PI * 2;
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double[] v = voronoi(p, x + 0.5, y + 0.5);
			int bi = (int) v[2];
			double t;
			if (v[1] - v[0] < 0.8) t = 0.92;                                   // ridge between facets
			else {
				double lam = Math.cos(facing[bi] - Math.toRadians(225));         // facet facing the top-left light
				t = 0.54 + lam * 0.18 + (x - y) / 64.0 - v[0] * 0.015;
				if (v[1] - v[0] < 1.6) t -= 0.12;                                // shadowed foot of the ridge
			}
			img.setRGB(x, y, argb(rampQ(body(pal), t + (hash(x, y, seed) - 0.5) * 0.06)));
		}
		for (int i = 0; i < 5; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(pal[pal.length - 1]));
		}
		return img;
	}

	/** Bark: vertical ridges with deep crevices, lit ridge flanks and glowing sap running in a few cracks. */
	static BufferedImage logSide(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2}, new double[]{0.5, 0.3, 0.2});
		double[] phase = new double[16];
		for (int x = 0; x < 16; x++) phase[x] = rng.nextDouble() * 0.6;
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double r = Math.sin((x + phase[x] + Math.sin(y * 0.4 + x) * 0.5) * Math.PI * 2 / 5.33);
			double t = 0.45 + r * 0.24 + (n[x][y] - 0.5) * 0.3;
			double rl = Math.sin((x - 1 + phase[(x + 15) % 16]) * Math.PI * 2 / 5.33);
			if (r < -0.7) t = 0.04;                    // crevice
			else if (rl < -0.7) t += 0.18;             // lit flank right of a crevice
			img.setRGB(x, y, argb(rampQ(pal, t)));
		}
		// horizontal breaks
		for (int i = 0; i < 3; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(pal[0]));
			img.setRGB((x + 1) & 15, y, argb(pal[0]));
			img.setRGB(x, (y + 1) & 15, argb(pal[pal.length - 1]));
		}
		// glowing sap veins
		for (int i = 0; i < 3; i++) {
			int x = rng.nextInt(16), y = rng.nextInt(16);
			img.setRGB(x, y, argb(0xc9fff0));
			img.setRGB(x, (y + 1) & 15, argb(0x7ff5da));
			img.setRGB(x, (y + 2) & 15, argb(0x2bbfa9));
		}
		return img;
	}

	static BufferedImage logTop(int[] bark, int[] wood, long seed) {
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			if (x == 0 || y == 0 || x == 15 || y == 15) {
				img.setRGB(x, y, argb(rampQ(bark, 0.3 + hash(x, y, seed) * 0.4)));
				continue;
			}
			if (x == 1 || y == 1 || x == 14 || y == 14) {
				img.setRGB(x, y, argb(rampQ(bark, x == 14 || y == 14 ? 0.15 : 0.75)));
				continue;
			}
			double dx = x - 7.5, dy = y - 7.5;
			double d = Math.max(Math.abs(dx), Math.abs(dy)) * 0.65 + Math.hypot(dx, dy) * 0.45 + Math.sin(Math.atan2(dy, dx) * 3) * 0.25;
			double ring = d % 1.6 / 1.6;
			double t = 0.35 + ring * 0.45 + (dx + dy < 0 ? 0.08 : -0.04);
			if (ring < 0.18) t = 0.12;
			img.setRGB(x, y, argb(rampQ(wood, t)));
			if (Math.hypot(dx, dy) < 1.3) img.setRGB(x, y, argb(Math.hypot(dx, dy) < 0.8 ? 0xc9fff0 : 0x7ff5da));
		}
		// radial crack
		for (int i = 3; i < 7; i++) img.setRGB(8 + i, 8 - i / 3, argb(wood[0]));
		return img;
	}

	/** Planks: four boards with their own tone, wavy grain, bevelled edges, butt joints and nails. */
	static BufferedImage planks(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 2}, new double[]{0.6, 0.4});
		double[] tone = new double[4];
		int[] seams = new int[4];
		for (int i = 0; i < 4; i++) {
			tone[i] = rng.nextDouble() * 0.16 - 0.08;
			seams[i] = (i % 2 == 0 ? 10 : 3) + rng.nextInt(3);
		}
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int board = y / 4, ly = y % 4;
			double t;
			if (ly == 3) t = 0.04;
			else if (x == seams[board]) t = 0.12;
			else {
				double grain = Math.sin(x * 0.55 + board * 2.3 + n[x][y] * 5 + ly * 0.9);
				t = 0.5 + tone[board] + grain * 0.12;
				if (ly == 0) t += 0.18;
				else if (ly == 2) t -= 0.1;
				if (x == seams[board] + 1) t += 0.12;
				if (Math.abs(grain) > 0.96) t -= 0.14; // grain line
			}
			img.setRGB(x, y, argb(rampQ(pal, t)));
		}
		for (int board = 0; board < 4; board++) { // nails next to the joints
			int ny = board * 4 + 1;
			img.setRGB((seams[board] + 2) & 15, ny, argb(0xd8d0e0));
			img.setRGB((seams[board] + 14) & 15, ny, argb(0xd8d0e0));
		}
		return img;
	}

	/** Leaves: overlapping shaded leaf blobs with holes, lit on the upper-left, plus a few glowing buds. */
	static BufferedImage leaves(int[] pal, long seed) {
		rng = new Random(seed);
		BufferedImage img = img(16, 16);
		int[] b = java.util.Arrays.copyOf(pal, pal.length - 2);
		for (int i = 0; i < 46; i++) {
			double cx = rng.nextDouble() * 16, cy = rng.nextDouble() * 16;
			double ang = rng.nextInt(4) * Math.PI / 4, rx = 2.1, ry = 1.2;
			double tone = rng.nextDouble() * 0.25;
			for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
				double u = (dx * Math.cos(ang) + dy * Math.sin(ang)) / rx, v = (-dx * Math.sin(ang) + dy * Math.cos(ang)) / ry;
				double r = u * u + v * v;
				if (r > 1) continue;
				int x = Math.floorMod((int) Math.round(cx + dx), 16), y = Math.floorMod((int) Math.round(cy + dy), 16);
				double t = 0.22 + tone + (-dx - dy) * 0.08 + (1 - r) * 0.38;
				if (Math.abs(v) < 0.25 && Math.abs(u) < 0.8) t += 0.15; // leaf vein
				img.setRGB(x, y, argb(rampQ(b, t)));
			}
		}
		// thin out to get holes
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (hash(x, y, seed) > 0.94) img.setRGB(x, y, 0);
		for (int i = 0; i < 5; i++) {
			int x = rng.nextInt(15), y = rng.nextInt(15);
			img.setRGB(x, y, argb(pal[pal.length - 1]));
			img.setRGB(x + 1, y, argb(pal[pal.length - 2]));
		}
		return img;
	}

	static BufferedImage flower(int[] stemPal, int[] petal, long seed) {
		BufferedImage img = img(16, 16);
		for (int y = 7; y < 16; y++) img.setRGB(7 + (y > 12 ? 1 : 0), y, argb(stemPal[y % 2 == 0 ? 1 : 2]));
		// two leaves
		img.setRGB(6, 11, argb(stemPal[3])); img.setRGB(5, 10, argb(stemPal[3])); img.setRGB(5, 11, argb(stemPal[2])); img.setRGB(4, 10, argb(stemPal[1]));
		img.setRGB(9, 13, argb(stemPal[2])); img.setRGB(10, 12, argb(stemPal[3])); img.setRGB(10, 13, argb(stemPal[1])); img.setRGB(11, 12, argb(stemPal[2]));
		int cx = 7, cy = 4;
		for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
			double d = Math.hypot(dx, dy), ang = Math.atan2(dy, dx);
			double petalR = 2.3 + Math.cos(ang * 5) * 1.0;
			if (d > petalR) continue;
			double t = 0.55 + (-dx - dy) * 0.06 - d / petalR * 0.25;
			if (Math.abs(Math.sin(ang * 5 / 2)) < 0.18 && d > 1) t -= 0.25; // gap between petals
			int c = d < 1 ? petal[5] : rampQ(java.util.Arrays.copyOfRange(petal, 1, 5), t);
			img.setRGB(cx + dx, cy + dy, argb(c));
		}
		img.setRGB(cx, cy, argb(0xffffff));
		img.setRGB(cx + 1, cy + 1, argb(petal[3]));
		return img;
	}

	static BufferedImage shadecap(int[] pal, int[] glow, long seed) {
		rng = new Random(seed);
		BufferedImage img = dirt(pal, seed);
		int[][] spots = {{3, 3, 2}, {11, 4, 2}, {6, 10, 3}, {13, 12, 1}, {1, 13, 1}};
		for (int[] s : spots) {
			for (int dy = -s[2] - 1; dy <= s[2] + 1; dy++) for (int dx = -s[2] - 1; dx <= s[2] + 1; dx++) {
				double d = Math.hypot(dx, dy);
				int x = (s[0] + dx) & 15, y = (s[1] + dy) & 15;
				if (d > s[2] + 1.2) continue;
				if (d > s[2] + 0.3) { // soft halo on the cap
					img.setRGB(x, y, argb(lerp(img.getRGB(x, y) & 0xFFFFFF, glow[0], 0.35)));
					continue;
				}
				int c = d < 0.8 ? glow[3] : (dx + dy < 0 ? glow[2] : d < s[2] - 0.4 ? glow[1] : glow[0]);
				img.setRGB(x, y, argb(c));
			}
		}
		return img;
	}

	static BufferedImage stem(int[] pal, long seed) {
		rng = new Random(seed);
		double[][] n = fbm(16, 16, seed, new int[]{8, 2}, new double[]{0.6, 0.4});
		BufferedImage img = img(16, 16);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double fib = Math.sin(x * 1.9 + n[x][y] * 2.5);
			double t = 0.5 + fib * 0.2 + (n[x][(y * 3) & 15] - 0.5) * 0.2;
			if (fib < -0.9) t = 0.1;
			img.setRGB(x, y, argb(rampQ(pal, t)));
		}
		return img;
	}

	static BufferedImage[] portal(int[] pal, long seed, int frames) {
		BufferedImage[] out = new BufferedImage[frames];
		double[][] n = fbm(16, 16, seed, new int[]{8, 4, 2}, new double[]{0.5, 0.3, 0.2});
		Random r = new Random(seed);
		double[][] stars = new double[6][3];
		for (double[] s : stars) {
			s[0] = r.nextDouble() * 6.3;
			s[1] = 1 + r.nextDouble() * 6;
			s[2] = r.nextDouble();
		}
		for (int f = 0; f < frames; f++) {
			BufferedImage img = img(16, 16);
			double t = f / (double) frames * Math.PI * 2;
			for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
				double dx = x - 7.5, dy = y - 7.5;
				double rr = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
				double v = Math.sin(a * 3 + rr * 0.9 - t) * 0.5 + 0.5;
				v = v * 0.65 + n[x][y] * 0.35 + Math.sin(t + n[x][y] * 6) * 0.08;
				v = Math.pow(clamp01(v), 1.3);
				int c = ramp(pal, v);
				int alpha = 165 + (int) (v * 80);
				img.setRGB(x, y, (alpha << 24) | (c & 0xFFFFFF));
			}
			// sparkles spiralling inwards
			for (double[] s : stars) {
				double ph = (s[2] + f / (double) frames) % 1.0;
				double rad = s[1] * (1 - ph), ang = s[0] + ph * 4;
				int x = (int) Math.round(7.5 + Math.cos(ang) * rad), y = (int) Math.round(7.5 + Math.sin(ang) * rad);
				if (x >= 0 && y >= 0 && x < 16 && y < 16) img.setRGB(x, y, 0xF0FFFFFF);
			}
			out[f] = img;
		}
		return out;
	}

	/** Boss altar: bevelled metal frame with rivets around a brick core, gem socket on top, glowing seam on the sides. */
	static BufferedImage altar(int[] stone, int[] metal, int[] gem, long seed, boolean top) {
		BufferedImage img = bricks(stone, seed);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			double t = -1;
			if (x == 0 || y == 0 || x == 15 || y == 15) t = 0.1;
			else if (x == 1 || y == 1) t = 0.9;
			else if (x == 14 || y == 14) t = 0.3;
			else if (x == 2 || y == 2) t = 0.15; // shadow cast inwards
			if (t >= 0) img.setRGB(x, y, argb(rampQ(metal, t + (hash(x, y, seed) - 0.5) * 0.08)));
			else if (x == 13 || y == 13) img.setRGB(x, y, argb(shadeColor(img.getRGB(x, y) & 0xFFFFFF, 1.1)));
		}
		for (int[] c : new int[][]{{1, 1}, {14, 1}, {1, 14}, {14, 14}}) img.setRGB(c[0], c[1], argb(gem[4]));
		if (top) {
			for (int y = 3; y < 13; y++) for (int x = 3; x < 13; x++) {
				double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
				if (d < 1.2) img.setRGB(x, y, argb(gem[5]));
				else if (d < 2.8) img.setRGB(x, y, argb(dx + dy < 0 ? gem[4] : dx > 0 && dy > 0 ? gem[1] : gem[3]));
				else if (d < 3.5) img.setRGB(x, y, argb(rampQ(metal, dx + dy < 0 ? 0.2 : 0.85)));
				else if (d > 4.2 && d < 5.2) img.setRGB(x, y, argb(rampQ(metal, (dx + dy < 0 ? 0.75 : 0.4) + ((x + y) & 1) * 0.1)));
			}
		} else {
			for (int y = 4; y < 12; y++) {
				img.setRGB(7, y, argb(gem[y == 7 || y == 8 ? 5 : 3]));
				img.setRGB(8, y, argb(gem[y == 7 || y == 8 ? 4 : 2]));
				img.setRGB(6, y, argb(rampQ(metal, 0.25)));
				img.setRGB(9, y, argb(rampQ(metal, 0.75)));
			}
			for (int x = 4; x < 12; x++) {
				img.setRGB(x, 3, argb(rampQ(metal, 0.9)));
				img.setRGB(x, 4, argb(rampQ(metal, 0.55)));
				img.setRGB(x, 11, argb(rampQ(metal, 0.5)));
				img.setRGB(x, 12, argb(rampQ(metal, 0.2)));
			}
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
		save(sword(EMBER, GOLD, SHADOW_STEEL, SW_EMBER), "item/ember_greatsword");
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
	static int partId(char ch) {
		if (ch == '.') return -1;
		return switch (Character.toLowerCase(ch)) {
			case 'h' -> 1;
			case 'g' -> 2;
			default -> 0;
		};
	}

	/**
	 * Shade a sprite mask: every part (x = main material, h = handle/trim, g = gem) gets its own rounded bevel lit from
	 * the top-left, material noise and a soft sprite-wide gradient; digits 0-5 force a shade of the main material.
	 * Upper-case letters are one step brighter. The outline takes the colour of the neighbouring part.
	 */
	static BufferedImage shadeMask(char[][] m, int[] mat, int[] mat2, int[] mat3) {
		BufferedImage img = img(16, 16);
		int[][] id = new int[16][16];
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) id[y][x] = partId(m[y][x]);
		double[][] hgt = new double[16][16];
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			if (id[y][x] < 0) continue;
			double best = Math.min(Math.min(x + 1, y + 1), Math.min(16 - x, 16 - y));
			for (int yy = Math.max(0, y - 3); yy <= Math.min(15, y + 3); yy++)
				for (int xx = Math.max(0, x - 3); xx <= Math.min(15, x + 3); xx++)
					if (id[yy][xx] != id[y][x]) best = Math.min(best, Math.hypot(xx - x, yy - y));
			hgt[y][x] = Math.min(best, 3);
		}
		long seed = mat[2] ^ (mat2[1] * 31L);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			char ch = m[y][x];
			int part = id[y][x];
			if (part < 0) continue;
			int[] pal = part == 1 ? mat2 : part == 2 ? mat3 : mat;
			double t;
			if (Character.isDigit(ch)) t = (ch - '0') / 5.0;
			else {
				double hl = hgtAt(hgt, id, x - 1, y, part), hr = hgtAt(hgt, id, x + 1, y, part);
				double hu = hgtAt(hgt, id, x, y - 1, part), hd = hgtAt(hgt, id, x, y + 1, part);
				double lam = Math.max(-1, Math.min(1, ((hr - hl) + (hd - hu)) * 0.5));
				double depth = Math.min(hgt[y][x], 2.5) / 2.5;
				t = 0.44 + lam * 0.36 + depth * 0.1 + (0.5 - (x + y) / 30.0) * 0.16 + (hash(x, y, seed) - 0.5) * 0.12;
				if (part == 2) t += 0.12; // gems catch more light
				if (Character.isUpperCase(ch)) t += 0.14;
			}
			int c = rampQ(pal, t);
			// specular glint on the brightest rim pixels
			if (!Character.isDigit(ch) && t > 0.9 && hash(x, y, seed + 1) > 0.55) c = lerp(c, 0xffffff, 0.45);
			img.setRGB(x, y, argb(c));
		}
		outlineSel(img, 0.38);
		return img;
	}

	static double hgtAt(double[][] hgt, int[][] id, int x, int y, int part) {
		if (x < 0 || y < 0 || x > 15 || y > 15 || id[y][x] != part) return 0;
		return hgt[y][x];
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
		return sword(blade, guard, handle, legendary ? SW_RADIANT : SW_PLAIN);
	}

	static final int SW_PLAIN = 0, SW_RADIANT = 1, SW_EMBER = 2, SW_REAVER = 3, SW_FROST = 4, SW_SONIC = 5;

	/**
	 * Diagonal sword sprite painted in blade coordinates: a = across the blade (negative = lit upper-left edge),
	 * l = along it (tip at l = 13, guard at l = -5/-4, grip below, pommel at the lower-left corner).
	 */
	static BufferedImage sword(int[] blade, int[] guard, int[] handle, int style) {
		BufferedImage img = img(16, 16);
		boolean wide = style != SW_PLAIN;
		int[] glow = style == SW_RADIANT ? GLOWCRYSTAL : blade;
		int gw = wide ? 4 : 3;
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int a = x + y - 15, l = x - y;
			int c = -1;
			int maxA = l >= 12 ? 0 : l >= 10 ? 1 : (wide ? 2 : 1);
			double n = (hash(x, y, blade[1]) - 0.5) * 0.08;
			if (l >= -3 && l <= 13 && Math.abs(a) <= maxA) {
				double t;
				if (l == 13) t = 1.0;
				else if (maxA > 0 && a == -maxA) t = 0.96;        // honed edge catching the light
				else if (maxA > 0 && a == maxA) t = 0.3;          // edge in shadow
				else if (a == 0) t = wide ? 0.6 : 0.74;           // fuller groove / centre ridge
				else t = a < 0 ? 0.8 : 0.56;
				t += (l - 5) * 0.012 + n;
				c = rampQ(blade, t);
				if (wide && a == 0 && l > -3 && l < 10 && (l + 1) % 4 == 0) c = glow[glow.length - 2]; // runes in the fuller
				if (style == SW_SONIC && a == 0 && l > -3 && l < 11 && (l + 3) % 4 == 0) c = 0x05181c;     // hollow resonance slots
			}
			// style extras just outside the blade
			if (c == -1 && l >= -2 && l <= 10) {
				// (a + l is always odd, so a = 3 only exists on even l)
				if (style == SW_EMBER && a == maxA + 1 && Math.floorMod(l, 4) == 0) c = EMBER[4];      // flame tongues
				if (style == SW_REAVER && a == -(maxA + 1) && Math.floorMod(l, 4) == 2) c = blade[3];  // serrated teeth
				if (style == SW_FROST && a == maxA + 1 && Math.floorMod(l, 6) == 0) c = 0xe8f6ff;      // frost crystals
			}
			// crossguard
			if ((l == -5 || l == -4) && Math.abs(a) <= gw) {
				double t = (l == -4 ? 0.78 : 0.48) - a * 0.03 + n;
				if (Math.abs(a) >= gw - 1) t += 0.1;
				c = rampQ(guard, t);
				if (l == -5 && a == 0) c = glow[glow.length - 2];
				if (l == -4 && a == -1) c = glow[glow.length - 1];
				if (l == -4 && a == 1) c = glow[2];
				if (style == SW_FROST && Math.abs(a) == gw && l == -4) c = 0xe8f6ff;
			}
			if (style != SW_PLAIN && l == -3 && Math.abs(a) == gw) c = rampQ(guard, 0.85); // upturned quillons
			// wrapped grip
			if (l >= -11 && l <= -6 && (a == -1 || a == 0)) {
				double t = a < 0 ? 0.72 : 0.42;
				if (Math.floorMod(l, 4) >= 2) t -= 0.16; // leather wrap bands
				c = rampQ(handle, t + n);
			}
			// pommel
			if (l == -12 && Math.abs(a) <= 1) c = rampQ(guard, a < 0 ? 0.92 : 0.4);
			if (l == -13 && a == 0) c = glow[glow.length - 2];
			if (c != -1) img.setRGB(x, y, argb(c));
		}
		outlineSel(img, 0.36);
		if (style != SW_PLAIN) glowEdge(img, blade[4]);
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
		if (pull > 0) {
			// arrow along the diagonal, nock at the string
			int nock = 8 + (int) Math.round(pull * 0.9);
			for (int k = nock; k >= 3; k--) set(g, k, k, k <= 4 ? 'G' : (k >= nock - 1 ? '4' : 'x'));
		}
		int[] arrowPal = {0x3b2614, 0x553a1f, 0x6f4e2b, 0x8a663a, 0xd8e8f0, 0xffffff};
		BufferedImage img = shadeMask(g, arrowPal, wood, gem);
		// thin bright string from tip to tip (pulled toward the lower-right), drawn after the outline
		int ax = 2, ay = 13, bx = 13, by = 2;
		for (int i = 0; i <= 40; i++) {
			double t = i / 40.0;
			double off = Math.sin(t * Math.PI) * pull * 1.3;
			int px = (int) Math.round(ax + (bx - ax) * t + off), py = (int) Math.round(ay + (by - ay) * t + off);
			if (filled(g, px, py) || px < 0 || py < 0 || px > 15 || py > 15) continue;
			img.setRGB(px, py, argb(t < 0.5 ? 0xf2faff : 0xc8e2f5));
		}
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

	/** Ingot drawn as a small 3D bar: lit top face, mid front face, dark end face, stamped star on top. */
	static BufferedImage ingot(int[] pal, long seed) {
		BufferedImage img = img(16, 16);
		for (int y = 5; y <= 12; y++) for (int x = 1; x <= 14; x++) {
			double t = -1;
			if (y >= 5 && y <= 8) { // top face, slanting back to the right
				int x0 = 6 - (y - 5), x1 = 12 - (y - 5);
				if (x >= x0 && x <= x1) t = y == 5 ? 0.98 : 0.78 - (x - x0) * 0.015;
				else if (x > x1 && x <= 13 - (y - 5) && y > 5) t = 0.3; // end face
			}
			if (y >= 9 && y <= 11) { // front face
				if (x >= 2 && x <= 9) t = y == 9 ? 0.62 : y == 11 ? 0.32 : 0.48;
				else if (x >= 10 && x <= 13 - (y - 8)) t = 0.2;     // end face
			}
			if (t >= 0) img.setRGB(x, y, argb(rampQ(pal, t + (hash(x, y, seed) - 0.5) * 0.06)));
		}
		img.setRGB(8, 6, argb(pal[pal.length - 1]));
		img.setRGB(7, 7, argb(pal[pal.length - 2]));
		img.setRGB(9, 7, argb(pal[pal.length - 2]));
		img.setRGB(8, 7, argb(pal[pal.length - 1]));
		img.setRGB(3, 9, argb(pal[pal.length - 2]));
		outlineSel(img, 0.38);
		return img;
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
			case HELMET -> parse( // closed great helm: visor with eye slits and breathing holes
					"................",
					"................",
					"......hhhh......",
					"....xxxhhxxx....",
					"...xxxxhhxxxx...",
					"..xxxxxhhxxxxx..",
					"..hhhhhhhhhhhh..",
					"..x000xXXx000x..",
					"..xxxxxXXxxxxx..",
					"..xx0xxXXxx0xx..",
					"..xxxxxXXxxxxx..",
					"...x0xxXXxx0x...",
					"...xxxxggxxxx...",
					"....hhhhhhhh....");
			case CHEST -> parse(
					"................",
					"..hhhh....hhhh..",
					".hxxxxhhhhxxxxh.",
					".xxxxxxXXxxxxxx.",
					".xxxxxxggxxxxxx.",
					".xxx.xxggxx.xxx.",
					".xxx.xxXXxx.xxx.",
					".hhh.hhhhhh.hhh.",
					".....xxXXxx.....",
					".....hhGGhh.....",
					".....xxxxxx.....",
					".....xx..xx.....",
					"................");
			case LEGS -> parse(
					"................",
					"................",
					"....hhhhhhhh....",
					"....xxxGgxxx....",
					"....xxxxxxxx....",
					"....xxx..xxx....",
					"....XXX..XXX....",
					"....hgh..hgh....",
					"....xxx..xxx....",
					"....xxx..xxx....",
					"....xxx..xxx....",
					"....hhh..hhh....",
					"................");
			default -> parse(
					"................",
					"................",
					"................",
					"................",
					"................",
					"................",
					"...hhh....hhh...",
					"...xxx....xxx...",
					"...xgx....xgx...",
					"...xxx....xxx...",
					"..xxxx...xxxx...",
					".xxxxx..xxxxx...",
					".hhhhh..hhhhh...",
					"................");
		};
		BufferedImage img = shadeMask(g, mat, trim, GLOWCRYSTAL == mat ? GOLD : mat);
		if (type == HELMET) { // dark eye slits and breathing holes with a faint glow behind the eyes
			for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
				if (g[y][x] == '0') img.setRGB(x, y, argb(lerp(darken(mat[0], 0.3), 0x05030a, 0.5)));
			img.setRGB(4, 7, argb(lerp(mat[0], mat[mat.length - 2], 0.55)));
			img.setRGB(11, 7, argb(lerp(mat[0], mat[mat.length - 2], 0.55)));
		}
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
		save(sword(OBSIDIAN_P, VOIDSHARD, SHADOW_STEEL, SW_REAVER), "item/void_reaver");
		save(sword(ICE_P, IRON_P, FROST_HANDLE, SW_FROST), "item/frostbite_blade");
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
		save(sword(ECHO_CRYSTAL, SHADOW_STEEL, SHADOW_STEEL, SW_SONIC), "item/sonic_blade");
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
		// UV boxes of the vanilla humanoid model: head (0,0) 8x8x8, hat (32,0), body (16,16) 8x12x4, arm (40,16) and leg (0,16) 4x12x4.
		// Face ids of paintBox: 0 top, 1 front, 2 right, 3 back, 4 left, 5 bottom.
		int slit = lerp(darken(mat[0], 0.25), 0x05030a, 0.5);
		int eyeGlow = lerp(slit, mat[mat.length - 2], 0.55);
		if (!legs) {
			// ---- closed great helm: every face opaque, the face is hidden behind a visor
			paintBox(img, 0, 0, 8, 8, 8, (x, y, face, fx, fy, fw, fh) -> {
				double t = plate(n, x, y, fx, fy, fw, fh);
				switch (face) {
					case 0 -> { // dome with a crest ridge and rivets
						if (fx == 3 || fx == 4) return rampQ(trim, fx == 3 ? 0.85 : 0.5);
						if ((fx == 1 || fx == 6) && (fy == 1 || fy == 6)) return rampQ(trim, 0.9);
						return rampQ(mat, t + 0.12 - Math.hypot(fx - 3.5, fy - 3.5) * 0.04);
					}
					case 5 -> {
						return rampQ(mat, 0.08);
					}
					case 1 -> { // visor
						if (fy == 0 || fy == 7) return rampQ(trim, fy == 0 ? 0.88 : 0.45);   // brow and chin rims
						if (fy == 3) {                                                     // eye slits beside the nose guard
							if (fx == 1 || fx == 6) return slit;
							if (fx == 2 || fx == 5) return eyeGlow;
							if (fx == 3 || fx == 4) return rampQ(mat, fx == 3 ? 0.9 : 0.6);
							return rampQ(mat, 0.35);
						}
						if (fy == 2) return rampQ(mat, fx == 3 || fx == 4 ? 0.95 : 0.78);     // lit brow plate
						if (fy == 4) return rampQ(mat, fx == 3 || fx == 4 ? 0.7 : 0.28);      // shadow under the slit
						if ((fy == 5 || fy == 6) && (fx == 1 || fx == 6) && ((fx + fy) & 1) == 0) return slit; // breathing holes
						if (fx == 3 || fx == 4) return rampQ(mat, t + 0.22);                  // centre ridge
						return rampQ(mat, t);
					}
					case 3 -> { // neck guard lames
						if (fy == 0) return rampQ(trim, 0.8);
						if (fy == 7) return rampQ(trim, 0.4);
						if (fy == 4 || fy == 6) return rampQ(mat, 0.2);
						if (fx == 3 || fx == 4) return rampQ(mat, t + 0.18);
						return rampQ(mat, t + (fy == 5 ? 0.12 : 0));
					}
					default -> { // sides: ear disc, rim, breathing holes, seam towards the visor
						boolean frontEdge = face == 2 ? fx == 7 : fx == 0;
						if (fy == 0 || fy == 7) return rampQ(trim, fy == 0 ? 0.8 : 0.4);
						if (frontEdge) return rampQ(mat, 0.18);
						double d = Math.hypot(fx - 3.5, fy - 3.5);
						if (d < 0.8) return rampQ(trim, 0.95);
						if (d < 1.8) return rampQ(trim, fx + fy < 7 ? 0.7 : 0.35);
						if (fy == 6 && (fx == 2 || fx == 5)) return slit;
						return rampQ(mat, t);
					}
				}
			});
			// crest on the outer helmet layer (sits half a pixel above the helm)
			paintBox(img, 32, 0, 8, 8, 8, (x, y, face, fx, fy, fw, fh) -> {
				if (face == 0 && (fx == 3 || fx == 4)) return rampQ(trim, fx == 3 ? 0.95 : 0.6);
				if ((face == 1 || face == 3) && (fx == 3 || fx == 4) && fy <= (face == 1 ? 0 : 1)) return rampQ(trim, fx == 3 ? 0.85 : 0.5);
				return -1;
			});
			// ---- breastplate
			paintBox(img, 16, 16, 8, 12, 4, (x, y, face, fx, fy, fw, fh) -> {
				double t = plate(n, x, y, fx, fy, fw, fh);
				if (face == 0 || face == 5) return rampQ(face == 0 ? trim : mat, face == 0 ? 0.7 : 0.15);
				if (fy == 0) return rampQ(trim, 0.85);                                     // gorget rim
				if (fy == 9) return (face == 1 && (fx == 3 || fx == 4)) ? rampQ(trim, 0.95) : rampQ(LEATHER_P, 0.35); // belt + buckle
				if (fy == 6) return rampQ(trim, 0.6);
				if (fy == 7 || fy == 8 || fy >= 10) return rampQ(mat, (fy == 7 || fy == 10) ? 0.72 : 0.36); // fauld lames
				if (face == 1) {
					double dg = Math.abs(fx - 3.5) + Math.abs(fy - 3.5);
					if (dg < 1.1) return mat[mat.length - 1];                              // emblem gem
					if (dg < 2.1) return rampQ(trim, fx + fy < 7 ? 0.9 : 0.45);
					if (fx == 3 || fx == 4) return rampQ(mat, t + 0.2);                     // centre ridge
					return rampQ(mat, t + (fx < 4 ? 0.06 : -0.06) - Math.abs(fx - 3.5) * 0.03);
				}
				if (face == 3 && (fx == 3 || fx == 4)) return rampQ(mat, t + 0.16);          // spine
				if ((face == 2 || face == 4) && fx == 1) return rampQ(LEATHER_P, 0.4);       // side straps
				return rampQ(mat, t);
			});
			// ---- arms: pauldron, rerebrace, couter, vambrace, cuff
			paintBox(img, 40, 16, 4, 12, 4, (x, y, face, fx, fy, fw, fh) -> {
				double t = plate(n, x, y, fx, fy, fw, fh);
				if (face == 0) return (fx == 1 || fx == 2) && (fy == 1 || fy == 2) ? rampQ(trim, 0.95) : rampQ(mat, 0.75);
				if (face == 5) return rampQ(LEATHER_P, 0.3);
				if (fy == 0) return rampQ(trim, 0.9);
				if (fy == 3) return rampQ(trim, 0.5);
				if (fy == 1 || fy == 2) return rampQ(mat, fy == 1 ? 0.8 : 0.5);
				if (fy == 7) return (fx == 1 || fx == 2) ? rampQ(trim, 0.85) : rampQ(mat, 0.3);
				if (fy == 11) return rampQ(trim, 0.55);
				return rampQ(mat, t);
			});
			// ---- sabatons and greaves (upper leg left open so leggings stay visible)
			paintBox(img, 0, 16, 4, 12, 4, (x, y, face, fx, fy, fw, fh) -> {
				if (face == 0) return -1;
				if (face == 5) return rampQ(mat, 0.1);                                    // sole
				if (fy < 5) return -1;
				double t = plate(n, x, y, fx, fy - 5, fw, fh - 5);
				if (fy == 5) return rampQ(trim, 0.85);                                    // cuff
				if (fy == 11) return rampQ(mat, 0.12);
				if (fy == 9 && face == 1) return rampQ(trim, 0.6);                        // toe cap seam
				if (fy == 10 && face == 1) return rampQ(mat, 0.8);
				return rampQ(mat, t);
			});
		} else {
			// ---- leggings: belt and tassets over a quilted gambeson
			paintBox(img, 16, 16, 8, 12, 4, (x, y, face, fx, fy, fw, fh) -> {
				double t = plate(n, x, y, fx, fy, fw, fh);
				if (face == 0) return rampQ(LEATHER_P, 0.5);
				if (face == 5) return rampQ(mat, 0.15);
				if (fy < 7) return rampQ(mat, ((fx + fy) & 1) == 0 ? 0.32 : 0.22 + (fy == 0 ? 0.1 : 0)); // mail shirt under the belt
				if (fy == 7) return (face == 1 && (fx == 3 || fx == 4)) ? rampQ(trim, 0.95) : rampQ(trim, 0.45); // belt
				if (fy == 8) return rampQ(mat, 0.85);
				if (fx == 3 || fx == 4) return face == 1 ? rampQ(mat, 0.25) : rampQ(mat, t);  // split between tassets
				return rampQ(mat, t + (fy == 11 ? -0.25 : 0));
			});
			paintBox(img, 0, 16, 4, 12, 4, (x, y, face, fx, fy, fw, fh) -> {
				double t = plate(n, x, y, fx, fy, fw, fh);
				if (face == 0) return rampQ(mat, 0.6);
				if (face == 5) return rampQ(mat, 0.15);
				if (fy == 0) return rampQ(trim, 0.8);
				if (fy == 5 || fy == 6) {                                                     // knee cop
					if (face == 1 && (fx == 1 || fx == 2)) return rampQ(trim, fy == 5 ? 0.95 : 0.6);
					return rampQ(mat, fy == 5 ? 0.85 : 0.3);
				}
				if (fy == 11) return rampQ(trim, 0.45);
				if (face == 1 && fx == 1) return rampQ(mat, t + 0.15);                       // shin ridge
				return rampQ(mat, t);
			});
		}
		return img;
	}

	/** Plate metal tone for armor faces: noise plus a 1px bevel (lit top/left, shaded bottom/right). */
	static double plate(double[][] n, int x, int y, int fx, int fy, int fw, int fh) {
		double t = 0.5 + (n[x % 64][y % 32] - 0.5) * 0.35;
		if (fy == 0) t += 0.2;
		else if (fy == fh - 1) t -= 0.2;
		if (fx == 0) t += 0.1;
		else if (fx == fw - 1) t -= 0.14;
		return t;
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
				int c = p.paint(x, y, f[4], fx, fy, f[2], f[3]);
				img.setRGB(x, y, c < 0 ? 0 : argb(c)); // negative = leave transparent
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

	// ================================================================ 3D HELD WEAPONS
	/*
	 * Every weapon is built from cuboids in an "upright" frame: it points up (+y) along x = 8, z = 8 and is gripped
	 * around y = 0. Each element is then turned -45 degrees around the model centre (8, 8, 8), which lines it up with
	 * the diagonal 2D sprite, so the usual handheld / bow display transforms hold it exactly like the flat item.
	 * TextureGen paints the texture sheet item/<name>_3d.png; DataGen writes models/item/<name>_3d.json from the
	 * same definitions (models3d()), so UVs and textures always match.
	 */
	/** Texels per model unit on the 3D sheets: twice the detail of a 16x16 sprite. */
	static final int TEXELS = 2;
	static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

	/** A texel being painted: its point on the part (upright frame), face, place on the face and face size in texels. */
	record Tx(Part p, double x, double y, double z, int f, double u, double v, int i, int j, int w, int h) {
		boolean edgeFace() { return f == 4 || f == 5; }
		boolean flat() { return f == 2 || f == 3; }
		double ry() { return (y - p.y0) / Math.max(1e-6, p.y1 - p.y0); }
		/** -1 .. 1 across the part, mirrored on the north face so negative is always the viewer's left. */
		double rx() {
			double r = (x - (p.x0 + p.x1) / 2) / Math.max(1e-6, (p.x1 - p.x0) / 2);
			return f == 2 ? -r : r;
		}
		double n() { return hash3(x, y, z + f * 3.1); }
	}

	interface Paint {
		int color(Tx t);
	}

	static final class Part {
		final double x0, y0, z0, x1, y1, z1;
		final Paint paint;
		double tilt, pivotX = 8, pivotY = 8;
		int light;
		final int[][] rect = new int[6][];

		Part(double x0, double y0, double z0, double x1, double y1, double z1, Paint paint) {
			this.x0 = x0; this.y0 = y0; this.z0 = z0; this.x1 = x1; this.y1 = y1; this.z1 = z1;
			this.paint = paint;
		}

		Part tilt(double deg, double px, double py) {
			tilt = deg; pivotX = px; pivotY = py;
			return this;
		}

		Part light(int l) {
			light = l;
			return this;
		}

		int[] texels(int f) {
			double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
			double w = f <= 3 ? dx : dz, h = f <= 1 ? dz : dy;
			return new int[]{Math.max(1, (int) Math.round(w * TEXELS)), Math.max(1, (int) Math.round(h * TEXELS))};
		}

		/** Point on face f for face coordinates u, v (0..1, v = 0 at the top); matches Minecraft's UV orientation. */
		double[] point(int f, double u, double v) {
			double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
			return switch (f) {
				case 0 -> new double[]{x0 + u * dx, y0, z1 - v * dz};
				case 1 -> new double[]{x0 + u * dx, y1, z0 + v * dz};
				case 2 -> new double[]{x1 - u * dx, y1 - v * dy, z0};
				case 3 -> new double[]{x0 + u * dx, y1 - v * dy, z1};
				case 4 -> new double[]{x0, y1 - v * dy, z0 + u * dz};
				default -> new double[]{x1, y1 - v * dy, z1 - u * dz};
			};
		}

		/** Upright-frame tilt followed by the global -45 degree turn, applied to an (x, y) point. */
		double[] transform(double x, double y) {
			double rt = Math.toRadians(tilt), vx = x - pivotX, vy = y - pivotY;
			double ax = pivotX + vx * Math.cos(rt) - vy * Math.sin(rt), ay = pivotY + vx * Math.sin(rt) + vy * Math.cos(rt);
			double g = Math.toRadians(-45), bx = ax - 8, by = ay - 8;
			return new double[]{8 + bx * Math.cos(g) - by * Math.sin(g), 8 + bx * Math.sin(g) + by * Math.cos(g)};
		}
	}

	static final class Model3D {
		final String name, particle;
		final List<Part> parts = new ArrayList<>();
		String display = "handheld";
		int sheet;

		Model3D(String name, String particle) {
			this.name = name;
			this.particle = particle;
		}

		Model3D(String name) {
			this(name, name);
		}

		Part add(Part p) {
			parts.add(p);
			return p;
		}

		/** Box centred on x = cx, z = 8. */
		Part box(double cx, double y0, double w, double h, double d, Paint p) {
			return add(new Part(cx - w / 2, y0, 8 - d / 2, cx + w / 2, y0 + h, 8 + d / 2, p));
		}

		/** Square turned 45 degrees: a diamond whose corners are diag / 2 from (cx, cy) - used for pointed tips. */
		Part diamond(double cx, double cy, double diag, double d, Paint p) {
			double s = diag / Math.sqrt(2);
			return add(new Part(cx - s / 2, cy - s / 2, 8 - d / 2, cx + s / 2, cy + s / 2, 8 + d / 2, p)).tilt(45, cx, cy);
		}

		/** Bar from point A to point B with thickness th and depth d (its local +y runs from A to B). */
		Part beam(double ax, double ay, double bx, double by, double th, double d, Paint p) {
			double len = Math.hypot(bx - ax, by - ay), mx = (ax + bx) / 2, my = (ay + by) / 2;
			Part s = add(new Part(mx - th / 2, my - len / 2, 8 - d / 2, mx + th / 2, my + len / 2, 8 + d / 2, p));
			return s.tilt(Math.toDegrees(Math.atan2(-(bx - ax), by - ay)), mx, my);
		}

		/** Chain of bars along a circular arc, a0 < a1 in degrees (counter-clockwise); local +x of each bar points outwards. */
		void arc(double cx, double cy, double r, double a0, double a1, int segs, double t0, double t1, double d, Paint p, int light) {
			for (int i = 0; i < segs; i++) {
				double s0 = Math.toRadians(a0 + (a1 - a0) * i / segs) - 0.12 / r, s1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / segs) + 0.12 / r;
				double th = t0 + (t1 - t0) * (i + 0.5) / segs;
				beam(cx + Math.cos(s0) * r, cy + Math.sin(s0) * r, cx + Math.cos(s1) * r, cy + Math.sin(s1) * r, th, d + (i % 2) * 0.03, p).light(light);
			}
		}

		void layout() {
			for (int size : new int[]{32, 64, 128}) {
				if (pack(size)) {
					sheet = size;
					return;
				}
			}
			throw new IllegalStateException("3D texture sheet too small for " + name);
		}

		/** Shelf packing of all faces with a 1px gap. */
		boolean pack(int size) {
			List<int[]> faces = new ArrayList<>();
			for (int i = 0; i < parts.size(); i++) for (int f = 0; f < 6; f++) {
				int[] wh = parts.get(i).texels(f);
				faces.add(new int[]{i, f, wh[0], wh[1]});
			}
			faces.sort((a, b) -> b[3] != a[3] ? b[3] - a[3] : b[2] - a[2]);
			int x = 0, y = 0, rowH = 0;
			for (int[] fc : faces) {
				if (x + fc[2] > size) {
					x = 0;
					y += rowH + 1;
					rowH = 0;
				}
				if (fc[2] > size || y + fc[3] > size) return false;
				parts.get(fc[0]).rect[fc[1]] = new int[]{x, y, fc[2], fc[3]};
				x += fc[2] + 1;
				rowH = Math.max(rowH, fc[3]);
			}
			return true;
		}

		BufferedImage paint() {
			layout();
			BufferedImage img = img(sheet, sheet);
			for (Part p : parts) for (int f = 0; f < 6; f++) {
				int[] r = p.rect[f];
				for (int j = 0; j < r[3]; j++) for (int i = 0; i < r[2]; i++) {
					double u = (i + 0.5) / r[2], v = (j + 0.5) / r[3];
					double[] pt = p.point(f, u, v);
					img.setRGB(r[0] + i, r[1] + j, argb(p.paint.color(new Tx(p, pt[0], pt[1], pt[2], f, u, v, i, j, r[2], r[3]))));
				}
			}
			return img;
		}

		/** The element model JSON (texture "0" is the generated sheet). */
		String json(String ns) {
			layout();
			StringBuilder sb = new StringBuilder("{\"textures\":{\"particle\":\"" + ns + ":item/" + particle + "\",\"0\":\"" + ns + ":item/" + name + "_3d\"},\"elements\":[");
			for (int k = 0; k < parts.size(); k++) {
				Part p = parts.get(k);
				double th = p.tilt - 45;
				th = ((th % 360) + 540) % 360 - 180;
				double[] t0 = p.transform(0, 0);
				double fx0 = p.x0, fy0 = p.y0, fx1 = p.x1, fy1 = p.y1;
				String rot = "";
				double rad = Math.toRadians(th), cos = Math.cos(rad), sin = Math.sin(rad);
				if (Math.abs(sin) < 1e-9 && cos > 0) {
					fx0 += t0[0]; fx1 += t0[0]; fy0 += t0[1]; fy1 += t0[1];
				} else {
					// fixed point q of v -> R(th) v + t0
					double a = 1 - cos, b = sin, c = -sin, det = a * a - b * c;
					double qx = (a * t0[0] - b * t0[1]) / det, qy = (-c * t0[0] + a * t0[1]) / det;
					rot = ",\"rotation\":{\"origin\":[" + f(qx) + "," + f(qy) + ",8],\"axis\":\"z\",\"angle\":" + f(th) + "}";
				}
				for (double v : new double[]{fx0, fy0, p.z0, fx1, fy1, p.z1})
					if (v < -16 || v > 32) throw new IllegalStateException(name + ": element outside -16..32");
				if (k > 0) sb.append(",");
				sb.append("{\"from\":[").append(f(fx0)).append(",").append(f(fy0)).append(",").append(f(p.z0)).append("],\"to\":[").append(f(fx1)).append(",")
						.append(f(fy1)).append(",").append(f(p.z1)).append("]").append(rot);
				if (p.light > 0) sb.append(",\"light_emission\":").append(p.light);
				sb.append(",\"faces\":{");
				for (int fc = 0; fc < 6; fc++) {
					int[] r = p.rect[fc];
					double s = 16.0 / sheet;
					if (fc > 0) sb.append(",");
					sb.append("\"").append(FACES[fc]).append("\":{\"uv\":[").append(f(r[0] * s)).append(",").append(f(r[1] * s)).append(",").append(f((r[0] + r[2]) * s))
							.append(",").append(f((r[1] + r[3]) * s)).append("],\"texture\":\"#0\"}");
				}
				sb.append("}}");
			}
			sb.append("],\"display\":").append(displayJson()).append("}");
			return sb.toString();
		}

		String displayJson() {
			String common = "\"ground\":{\"rotation\":[0,0,0],\"translation\":[0,2,0],\"scale\":[0.5,0.5,0.5]},"
					+ "\"head\":{\"rotation\":[0,180,0],\"translation\":[0,13,7],\"scale\":[1,1,1]},"
					+ "\"fixed\":{\"rotation\":[0,180,0],\"translation\":[0,0,0],\"scale\":[1,1,1]},"
					+ "\"gui\":{\"rotation\":[0,0,0],\"translation\":[0,0,0],\"scale\":[1,1,1]},"
					+ "\"firstperson_righthand\":{\"rotation\":[0,-90,25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]},"
					+ "\"firstperson_lefthand\":{\"rotation\":[0,90,-25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]}";
			if (display.equals("bow"))
				return "{\"thirdperson_righthand\":{\"rotation\":[-80,260,-40],\"translation\":[-1,-2,2.5],\"scale\":[0.9,0.9,0.9]},"
						+ "\"thirdperson_lefthand\":{\"rotation\":[-80,-280,40],\"translation\":[-1,-2,2.5],\"scale\":[0.9,0.9,0.9]}," + common + "}";
			return "{\"thirdperson_righthand\":{\"rotation\":[0,-90,55],\"translation\":[0,4,0.5],\"scale\":[0.85,0.85,0.85]},"
					+ "\"thirdperson_lefthand\":{\"rotation\":[0,90,-55],\"translation\":[0,4,0.5],\"scale\":[0.85,0.85,0.85]}," + common + "}";
		}

		static String f(double v) {
			double r = Math.round(v * 10000) / 10000.0;
			if (r == Math.rint(r)) return Long.toString((long) r);
			return Double.toString(r);
		}
	}

	// ------------------------------------------------------------ 3D materials
	static Paint bladeP(int[] pal) {
		return t -> {
			double n = (t.n() - 0.5) * 0.1, tt;
			if (t.edgeFace()) tt = 0.9;                       // honed edge
			else if (!t.flat()) tt = 0.74;
			else {
				double a = t.rx();
				if (Math.abs(a) > 0.7) tt = a < 0 ? 1.0 : 0.66;   // bevelled cutting edges
				else if (Math.abs(a) > 0.5) tt = 0.4;            // bevel line
				else tt = 0.6 - a * 0.14;
				tt += (t.ry() - 0.5) * 0.14;
			}
			return rampQ(pal, tt + n);
		};
	}

	static Paint ridgeP(int[] pal, int[] glow, boolean runes) {
		return t -> {
			if (runes && t.flat() && Math.floorMod((int) Math.floor(t.y * TEXELS), 6) == 2) return glow[glow.length - 2];
			double tt = t.flat() ? 0.82 : t.edgeFace() ? 0.46 : 0.7;
			return rampQ(pal, tt + (t.n() - 0.5) * 0.1 + (t.ry() - 0.5) * 0.1);
		};
	}

	static Paint tipP(int[] pal) {
		return t -> rampQ(pal, (t.flat() ? 0.92 - (t.u + t.v) * 0.12 : 0.85) + (t.n() - 0.5) * 0.08);
	}

	static Paint crystalP(int[] pal) {
		return t -> {
			double fy = t.y * 0.6 + t.x * 0.25, fx = t.x * 0.7 - t.y * 0.2 + t.z * 0.4;
			int cy = (int) Math.floor(fy), cx = (int) Math.floor(fx + cy * 0.5);
			double tt = 0.4 + hash(cx, cy + t.f * 7, pal[1]) * 0.42;
			double fr = fy - cy;
			if (fr < 0.14) tt += 0.28;                        // bright facet edge
			else if (fr > 0.86) tt -= 0.12;
			if (t.edgeFace()) tt += 0.08;
			if (t.n() > 0.965) tt = 1.05;                      // sparkle
			return rampQ(pal, tt + (t.n() - 0.5) * 0.06);
		};
	}

	static Paint metalP(int[] pal) {
		return t -> {
			double tt = 0.55 + (t.n() - 0.5) * 0.14;
			if (t.f == 1) tt += 0.18;
			else if (t.f == 0) tt -= 0.22;
			else if (t.edgeFace()) tt -= 0.05;
			if (t.w >= 3 && t.h >= 3) {
				if (t.j == 0) tt += 0.24;
				else if (t.j == t.h - 1) tt -= 0.2;
				else if (t.i == 0) tt += 0.1;
				else if (t.i == t.w - 1) tt -= 0.12;
			} else if (t.h >= 3 && t.j == 0) tt += 0.15;
			return rampQ(pal, tt);
		};
	}

	static Paint gripP(int[] pal) {
		return t -> {
			if (!t.flat() && !t.edgeFace()) return rampQ(pal, 0.4);
			int k = Math.floorMod(t.h - t.j + t.i, 3);
			double tt = k == 0 ? 0.2 : k == 1 ? 0.5 : 0.68;
			return rampQ(pal, tt + (t.n() - 0.5) * 0.08 - (t.edgeFace() ? 0.06 : 0));
		};
	}

	static Paint woodP(int[] pal) {
		return t -> {
			if (t.f <= 1) return rampQ(pal, 0.62 - Math.hypot(t.u - 0.5, t.v - 0.5) * 0.4);
			double g = Math.sin((t.x + t.z) * 4.2 + Math.sin(t.y * 0.8) * 1.6) * 0.5 + 0.5;
			double tt = 0.34 + g * 0.3 + (t.n() - 0.5) * 0.1 + (t.edgeFace() ? -0.05 : 0.04);
			if (t.n() > 0.975) tt -= 0.22; // knots
			return rampQ(pal, tt);
		};
	}

	static Paint boneP(int[] pal) {
		return t -> {
			double tt = 0.62 + (t.n() - 0.5) * 0.16 + (t.edgeFace() ? -0.1 : 0);
			if (Math.floorMod((int) Math.floor(t.y * TEXELS), 7) == 0) tt -= 0.22; // joints
			if (!t.flat() && !t.edgeFace()) tt = 0.72;
			return rampQ(pal, tt);
		};
	}

	static Paint gemP(int[] pal) {
		return t -> {
			double d = Math.hypot(t.u - 0.36, t.v - 0.36);
			double tt = 1.02 - d * 1.05;
			if ((t.i == 0 || t.j == 0) && t.w > 1 && t.h > 1) tt += 0.08;
			return rampQ(pal, tt);
		};
	}

	static Paint glowP(int[] pal) {
		return t -> rampQ(pal, 0.82 + (t.n() - 0.5) * 0.2 + (t.flat() ? 0.06 : 0));
	}

	static Paint rockP(int[] rock, int[] lava) {
		return t -> {
			double crack = Math.sin(t.x * 2.3 + t.y * 1.7 + t.z * 0.9) * Math.cos(t.y * 2.1 - t.z * 1.9 + t.x * 0.6);
			if (crack > 0.62) return rampQ(lava, 0.7 + crack * 0.3);
			double tt = 0.3 + t.n() * 0.35 + (t.f == 1 ? 0.15 : t.f == 0 ? -0.15 : 0);
			return rampQ(rock, tt);
		};
	}

	/** Curved blade (arc bars): local -x side (west face) is the inner, sharpened edge. */
	static Paint arcBladeP(int[] pal) {
		return t -> {
			double n = (t.n() - 0.5) * 0.1;
			if (t.f == 4) return rampQ(pal, 1.0 + n);
			if (t.f == 5) return rampQ(pal, 0.36 + n);
			if (!t.flat()) return rampQ(pal, 0.62 + n);
			double a = (t.x - (t.p.x0 + t.p.x1) / 2) / Math.max(1e-6, (t.p.x1 - t.p.x0) / 2);
			double tt = a < -0.45 ? 0.95 : a > 0.6 ? 0.36 : 0.6 - a * 0.12;
			return rampQ(pal, tt + n);
		};
	}

	static Paint stringP() {
		return t -> lerp(0xd8eaf8, 0xffffff, t.n() * 0.6);
	}

	static Paint featherP(int[] pal) {
		return t -> rampQ(pal, 0.55 + t.ry() * 0.4 + (Math.floorMod(t.i, 2) == 0 ? -0.1 : 0.05));
	}

	static final int[] DAGGER_STEEL = {0x2d273b, 0x4a4060, 0x6e6390, 0x9a8fc0, 0xcfc6ee, 0xffffff};

	// ------------------------------------------------------------ 3D weapon definitions
	static List<Model3D> models3d() {
		List<Model3D> out = new ArrayList<>();
		out.add(sword3d("glowcrystal_sword", GLOWCRYSTAL, GOLD, WOOD, GLOWCRYSTAL, SW_PLAIN, 6));
		out.add(sword3d("voidshard_sword", VOIDSHARD, SHADOW_STEEL, SHADOW_STEEL, VOIDSHARD, SW_PLAIN, 5));
		out.add(sword3d("radiant_blade", RADIANT, GOLD, GLOWCRYSTAL, GLOWCRYSTAL, SW_RADIANT, 10));
		out.add(sword3d("ember_greatsword", EMBER, GOLD, SHADOW_STEEL, EMBER, SW_EMBER, 9));
		out.add(sword3d("void_reaver", OBSIDIAN_P, VOIDSHARD, SHADOW_STEEL, VOIDSHARD, SW_REAVER, 0));
		out.add(sword3d("frostbite_blade", ICE_P, IRON_P, FROST_HANDLE, ICE_P, SW_FROST, 6));
		out.add(sword3d("sonic_blade", ECHO_CRYSTAL, SHADOW_STEEL, SHADOW_STEEL, ECHO_CRYSTAL, SW_SONIC, 8));
		out.add(dagger3d());
		out.add(scythe3d());
		out.add(hammer3d("star_hammer", STARMETAL, GLOWCRYSTAL, WOOD, false));
		out.add(hammer3d("infernal_maul", MAGMA_P, GOLD, SHADOW_STEEL, true));
		out.add(spear3d("thunder_spear", STORM_P, GOLD, WOOD, true));
		out.add(spear3d("sky_pike", GLOWCRYSTAL, GOLD, AURORA_WOOD, false));
		out.add(staff3d("aurora_staff", AURORA_WOOD, GLOWCRYSTAL, GOLD, 0));
		out.add(staff3d("bone_scepter", BONE_P, SOUL_P, GOLD, 1));
		out.add(staff3d("meteor_staff", SHADOW_STEEL, MAGMA_P, GOLD, 2));
		out.add(axe3d("glowcrystal_axe", GLOWCRYSTAL, WOOD, GOLD, false));
		out.add(axe3d("lumber_axe", GLOWCRYSTAL, AURORA_WOOD, GOLD, true));
		out.add(pick3d("glowcrystal_pickaxe", GLOWCRYSTAL, WOOD, GOLD, false));
		out.add(pick3d("voidshard_pickaxe", VOIDSHARD, SHADOW_STEEL, SHADOW_STEEL, false));
		out.add(pick3d("excavator_pickaxe", OBSIDIAN_P, SHADOW_STEEL, GOLD, true));
		out.add(shovel3d("glowcrystal_shovel", GLOWCRYSTAL, WOOD, GOLD));
		for (int pull = 0; pull <= 3; pull++) {
			String suffix = pull == 0 ? "" : "_pulling_" + (pull - 1);
			out.add(bow3d("crystal_bow" + suffix, AURORA_WOOD, GLOWCRYSTAL, pull));
			out.add(bow3d("storm_bow" + suffix, STORM_P, GOLD, pull));
		}
		return out;
	}

	static boolean crystalline(int[] pal) {
		return pal == GLOWCRYSTAL || pal == VOIDSHARD || pal == ECHO_CRYSTAL || pal == ICE_P || pal == RADIANT || pal == EMBER;
	}

	static Model3D sword3d(String name, int[] blade, int[] guard, int[] grip, int[] gem, int style, int glow) {
		Model3D m = new Model3D(name);
		boolean big = style == SW_RADIANT || style == SW_EMBER || style == SW_REAVER;
		double bw = big ? 3.5 : 2.5, top = big ? 19.5 : 18.5, gw = big ? 8 : 6.5, gy = 2.25;
		Paint metal = metalP(guard);
		m.box(8, -4.5, 2.25, 2, 2.25, metal);                          // pommel
		m.box(8, -4.0, 1, 1, 2.6, gemP(gem)).light(15);               // pommel gem
		m.box(8, -2.5, 1.5, 5.0, 1.5, gripP(grip));                   // wrapped grip
		m.box(8, gy, gw, 1.5, 2.0, metal);                             // crossguard
		m.box(8, gy - 0.5, 2.5, 2.5, 2.4, metal);                      // guard block
		m.box(8, gy + 0.25, 1.25, 1.5, 2.8, gemP(gem)).light(15);     // guard gem
		if (style == SW_PLAIN) {
			m.box(8 - gw / 2 + 0.45, gy - 0.25, 0.9, 2.0, 2.2, metal);  // guard end caps
			m.box(8 + gw / 2 - 0.45, gy - 0.25, 0.9, 2.0, 2.2, metal);
		} else {
			m.beam(8 - gw / 2 + 0.6, gy + 0.75, 8 - gw / 2 - 0.5, gy + 2.7, 1.0, 1.6, metal); // upswept quillons
			m.beam(8 + gw / 2 - 0.6, gy + 0.75, 8 + gw / 2 + 0.5, gy + 2.7, 1.0, 1.6, metal);
		}
		double by = gy + 1.5, bodyTop = top - bw / 2;
		Paint bp = crystalline(blade) && style == SW_PLAIN ? crystalP(blade) : bladeP(blade);
		m.box(8, by, bw, bodyTop - by, 0.75, bp).light(glow);
		m.box(8, by + 0.01, 0.75, bodyTop - by - 0.75, 1.25, ridgeP(blade, gem, style != SW_PLAIN)).light(glow);
		m.diamond(8, bodyTop, bw, 0.7, tipP(blade)).light(glow);
		switch (style) {
			case SW_RADIANT -> {
				m.diamond(8, gy + 1.0, 2.6, 2.9, gemP(GLOWCRYSTAL)).light(15);
				m.box(8, by + 1.5, bw + 0.5, 0.5, 1.0, metalP(GOLD));
			}
			case SW_EMBER -> {
				for (int i = 0; i < 4; i++) {
					double y = by + 2 + i * 3.1;
					m.beam(8 + bw / 2 - 0.3, y, 8 + bw / 2 + 1.0, y + 1.5, 0.75, 0.55, glowP(EMBER)).light(15);
					m.beam(8 - bw / 2 + 0.3, y + 1.5, 8 - bw / 2 - 0.8, y + 2.8, 0.6, 0.5, glowP(EMBER)).light(15);
				}
			}
			case SW_REAVER -> {
				for (int i = 0; i < 4; i++) m.diamond(8 - bw / 2, by + 2.5 + i * 3.2, 1.7, 0.6, bladeP(blade));
				m.box(8, by + 0.5, 0.5, bodyTop - by - 1.5, 1.4, glowP(VOIDSHARD)).light(15);
			}
			case SW_FROST -> {
				m.diamond(8 - gw / 2 - 0.2, gy + 0.75, 1.8, 1.4, crystalP(ICE_P)).light(10);
				m.diamond(8 + gw / 2 + 0.2, gy + 0.75, 1.8, 1.4, crystalP(ICE_P)).light(10);
				m.beam(8 + bw / 2 - 0.2, by + 4, 8 + bw / 2 + 1.1, by + 5.8, 0.7, 0.6, crystalP(ICE_P)).light(10);
				m.beam(8 - bw / 2 + 0.2, by + 8, 8 - bw / 2 - 1.0, by + 9.6, 0.6, 0.6, crystalP(ICE_P)).light(10);
			}
			case SW_SONIC -> {
				for (int i = 0; i < 3; i++) m.box(8, by + 2.5 + i * 3.8, bw + 0.5, 0.5, 1.4, glowP(ECHO_CRYSTAL)).light(15);
			}
			default -> {
			}
		}
		return m;
	}

	static Model3D dagger3d() {
		Model3D m = new Model3D("shadow_dagger");
		Paint metal = metalP(SHADOW_STEEL);
		m.box(8, -2.75, 1.9, 1.5, 1.9, metal);
		m.box(8, -2.4, 0.9, 0.8, 2.2, gemP(VOIDSHARD)).light(15);
		m.box(8, -1.25, 1.25, 3.75, 1.25, gripP(SHADOW_STEEL));
		m.box(8, 2.5, 4.75, 1.0, 1.75, metal);
		m.beam(8 - 2.1, 2.8, 8 - 2.9, 4.2, 0.8, 1.4, metal);
		m.beam(8 + 2.1, 2.8, 8 + 2.9, 4.2, 0.8, 1.4, metal);
		m.box(8, 2.6, 1.1, 1.1, 2.1, gemP(VOIDSHARD)).light(15);
		m.box(8, 3.5, 2.2, 8.5, 0.6, bladeP(DAGGER_STEEL));
		m.box(8, 3.51, 0.6, 8.0, 1.0, ridgeP(DAGGER_STEEL, VOIDSHARD, true));
		m.box(8 - 0.95, 3.8, 0.3, 7.8, 0.7, glowP(VOIDSHARD)).light(15);
		m.diamond(8, 12.0, 2.2, 0.55, tipP(DAGGER_STEEL));
		return m;
	}

	static Model3D scythe3d() {
		Model3D m = new Model3D("void_scythe");
		Paint metal = metalP(SHADOW_STEEL), wood = woodP(SHADOW_STEEL);
		m.box(8, -7, 1.5, 23.5, 1.5, wood);
		m.box(8, -5.25, 1.8, 3.0, 1.8, gripP(LEATHER_P));
		m.box(8, 4.5, 1.8, 2.25, 1.8, gripP(LEATHER_P));
		m.diamond(8, -7.25, 1.8, 1.3, metal);
		m.box(8, 14.0, 2.4, 2.75, 2.1, metal);
		m.box(8, 14.6, 1.0, 1.0, 2.5, gemP(VOIDSHARD)).light(15);
		// big crescent over the top, curving down to the left like the sprite
		m.arc(7.3, 8.0, 7.3, 80, 222, 11, 2.7, 0.5, 0.75, arcBladeP(VOIDSHARD), 8);
		m.beam(8.5, 15.8, 10.2, 18.2, 0.9, 0.9, metal);
		m.diamond(10.4, 18.4, 1.3, 0.8, gemP(VOIDSHARD)).light(15);
		return m;
	}

	static Model3D hammer3d(String name, int[] head, int[] gem, int[] handle, boolean maul) {
		Model3D m = new Model3D(name);
		double hw = maul ? 9.5 : 8.5, hh = maul ? 5.5 : 4.5, hd = maul ? 5.0 : 4.5, hy = 11;
		Paint trim = metalP(GOLD), body = maul ? rockP(new int[]{0x1a1010, 0x2a1818, 0x3a2020, 0x4a2a24, 0x5a3428}, MAGMA_P) : metalP(head);
		m.box(8, -5, 1.6, hy + 1 + 5, 1.6, woodP(handle));
		m.box(8, -3.5, 1.85, 4.5, 1.85, gripP(LEATHER_P));
		m.box(8, -5.75, 2.3, 1.25, 2.3, trim);
		m.box(8, hy - 1.25, 2.3, 1.5, 2.3, trim);
		m.box(8, hy, hw, hh, hd, body);
		m.box(8 - hw / 2 + 1.1, hy - 0.25, 1.0, hh + 0.5, hd + 0.5, trim);
		m.box(8 + hw / 2 - 1.1, hy - 0.25, 1.0, hh + 0.5, hd + 0.5, trim);
		m.box(8 - hw / 2 - 0.2, hy + 0.6, 0.6, hh - 1.2, hd - 1.2, metalP(maul ? SHADOW_STEEL : head));
		m.box(8 + hw / 2 + 0.2, hy + 0.6, 0.6, hh - 1.2, hd - 1.2, metalP(maul ? SHADOW_STEEL : head));
		m.box(8, hy + hh / 2 - 1, 2.0, 2.0, hd + 0.4, gemP(maul ? MAGMA_P : gem)).light(15);
		if (maul) {
			m.diamond(8 - hw / 2 - 0.9, hy + hh / 2, 2.4, 1.8, metalP(SHADOW_STEEL));
			m.diamond(8 + hw / 2 + 0.9, hy + hh / 2, 2.4, 1.8, metalP(SHADOW_STEEL));
			m.diamond(8, hy + hh, 2.2, 2.0, metalP(SHADOW_STEEL));
		} else {
			m.diamond(8, hy + hh, 2.6, 1.6, crystalP(gem)).light(12);
		}
		return m;
	}

	static Model3D spear3d(String name, int[] head, int[] trim, int[] shaft, boolean thunder) {
		Model3D m = new Model3D(name);
		Paint metal = metalP(trim);
		m.box(8, -7, 1.25, 27, 1.25, woodP(shaft));
		m.box(8, -2.75, 1.5, 5.5, 1.5, gripP(LEATHER_P));
		m.box(8, 3.0, 1.55, 0.6, 1.55, metal);
		m.box(8, -3.4, 1.55, 0.6, 1.55, metal);
		m.box(8, -7.75, 1.7, 1.0, 1.7, metal);
		m.diamond(8, -7.9, 1.6, 1.2, metal);
		m.box(8, 19.0, 1.9, 2.25, 1.9, metal);
		if (thunder) {
			m.box(8, 21.0, 3.6, 4.6, 0.75, bladeP(head)).light(6);
			m.box(8, 20.75, 0.75, 5.4, 1.25, ridgeP(head, head, true)).light(6);
			m.diamond(8, 25.6, 3.6, 0.7, tipP(head)).light(6);
			Paint bolt = glowP(new int[]{0xd1a21f, 0xffe066, 0xfff4b8, 0xffffff});
			for (int s = -1; s <= 1; s += 2) {
				m.beam(8 + s * 1.0, 20.3, 8 + s * 3.2, 22.0, 0.75, 0.6, bolt).light(15);
				m.beam(8 + s * 3.2, 22.0, 8 + s * 2.5, 23.6, 0.6, 0.62, bolt).light(15);
			}
		} else {
			m.box(8, 20.75, 2.2, 5.5, 0.7, crystalP(head)).light(8);
			m.diamond(8, 26.25, 2.2, 0.65, crystalP(head)).light(8);
			m.box(8, 20.4, 5.5, 0.8, 0.9, metal);
			m.diamond(8 - 2.9, 20.8, 1.4, 1.0, gemP(head)).light(15);
			m.diamond(8 + 2.9, 20.8, 1.4, 1.0, gemP(head)).light(15);
		}
		return m;
	}

	static Model3D staff3d(String name, int[] wood, int[] gem, int[] trim, int style) {
		Model3D m = new Model3D(name);
		Paint metal = metalP(trim);
		Paint shaft = style == 1 ? boneP(wood) : style == 2 ? metalP(wood) : woodP(wood);
		m.box(8, -6.5, 1.5, 20.5, 1.5, shaft);
		m.box(8, -7.1, 1.8, 0.75, 1.8, metal);
		m.box(8, 0.6, 1.75, 0.6, 1.75, metal);
		m.box(8, 7.6, 1.75, 0.6, 1.75, metal);
		if (style != 1) m.box(8, -2.5, 1.7, 3.5, 1.7, gripP(LEATHER_P));
		m.box(8, 13.25, 2.6, 1.5, 2.6, metal);
		Paint prong = style == 1 ? boneP(wood) : style == 2 ? metalP(SHADOW_STEEL) : metal;
		m.beam(7.0, 14.2, 5.5, 18.8, 0.85, 0.85, prong);
		m.beam(9.0, 14.2, 10.5, 18.8, 0.85, 0.85, prong);
		m.add(new Part(7.6, 14.5, 9.7, 8.4, 18.3, 10.5, prong));
		m.add(new Part(7.6, 14.5, 5.5, 8.4, 18.3, 6.3, prong));
		if (style == 2) {
			m.box(8, 15.0, 3.6, 3.6, 3.6, rockP(new int[]{0x1a1010, 0x2a1818, 0x3a2020, 0x4a2a24, 0x5a3428}, MAGMA_P));
			m.diamond(8, 16.8, 4.6, 2.4, glowP(MAGMA_P)).light(15);
		} else {
			m.box(8, 15.0, 3.8, 3.8, 3.8, gemP(gem)).light(15);
			m.diamond(8, 16.9, 5.0, 3.1, gemP(gem)).light(15);
		}
		if (style == 1) { // little horns on the bone scepter
			m.beam(5.6, 18.6, 4.6, 20.0, 0.6, 0.6, boneP(wood));
			m.beam(10.4, 18.6, 11.4, 20.0, 0.6, 0.6, boneP(wood));
		}
		return m;
	}

	static Model3D axe3d(String name, int[] head, int[] handle, int[] trim, boolean dbl) {
		Model3D m = new Model3D(name);
		Paint metal = metalP(trim);
		m.box(8, -4.75, 1.5, 19.25, 1.5, woodP(handle));
		m.box(8, -5.25, 1.9, 0.75, 1.9, metal);
		m.box(8, -3.25, 1.75, 3.0, 1.75, gripP(LEATHER_P));
		m.box(8, 10.25, 2.6, 4.0, 2.2, metal);
		m.box(8, 14.25, 1.9, 0.75, 1.9, metal);
		axeBlade(m, -1, head);
		if (dbl) axeBlade(m, 1, head);
		else m.diamond(10.4, 12.25, 2.2, 1.2, metal);
		return m;
	}

	static void axeBlade(Model3D m, int side, int[] head) {
		Paint body = crystalline(head) ? crystalP(head) : metalP(head);
		int light = crystalline(head) ? 6 : 0;
		m.box(8 + side * 3.05, 10.25, 3.7, 4.0, 0.9, body).light(light);
		m.beam(8 + side * 1.3, 13.9, 8 + side * 5.4, 15.7, 1.4, 0.85, body).light(light);
		m.beam(8 + side * 1.3, 10.6, 8 + side * 5.4, 8.8, 1.4, 0.85, body).light(light);
		m.box(8 + side * 5.55, 8.3, 1.1, 7.8, 0.55, tipP(head)).light(light);
	}

	static Model3D pick3d(String name, int[] head, int[] handle, int[] trim, boolean big) {
		Model3D m = new Model3D(name);
		Paint metal = metalP(trim);
		Paint body = crystalline(head) ? crystalP(head) : metalP(head);
		int light = crystalline(head) ? 6 : 0;
		double r = big ? 9.5 : 8.5, cy = 14.4 - r;
		m.box(8, -4.75, 1.5, 18.5, 1.5, woodP(handle));
		m.box(8, -5.25, 1.9, 0.75, 1.9, metal);
		m.box(8, -3.25, 1.75, 3.0, 1.75, gripP(LEATHER_P));
		m.box(8, 11.75, 2.6, 3.0, 2.2, metal);
		m.arc(8, cy, r, 28, 90, 6, 1.2, big ? 3.2 : 2.8, 1.6, body, light);
		m.arc(8, cy, r, 90, 152, 6, big ? 3.2 : 2.8, 1.2, 1.62, body, light);
		double a0 = Math.toRadians(28), a1 = Math.toRadians(152);
		m.diamond(8 + Math.cos(a0) * r, cy + Math.sin(a0) * r, 1.3, 1.2, tipP(head)).light(light);
		m.diamond(8 + Math.cos(a1) * r, cy + Math.sin(a1) * r, 1.3, 1.2, tipP(head)).light(light);
		if (big) m.box(8, 13.0, 3.4, 2.2, 2.6, metal);
		return m;
	}

	static Model3D shovel3d(String name, int[] head, int[] handle, int[] trim) {
		Model3D m = new Model3D(name);
		Paint metal = metalP(trim), body = crystalP(head);
		m.box(8, -4.75, 1.5, 15, 1.5, woodP(handle));
		m.box(8, -5.25, 1.9, 0.75, 1.9, metal);
		m.box(8, -3.25, 1.75, 3.0, 1.75, gripP(LEATHER_P));
		m.box(8, 9.25, 2.0, 2.25, 1.75, metal);
		m.box(8, 10.75, 4.5, 4.5, 0.75, body).light(6);
		m.box(8, 10.5, 4.8, 0.6, 0.95, metal);
		m.diamond(8, 15.25, 4.5, 0.7, body).light(6);
		m.box(8, 10.76, 0.8, 5.0, 1.05, ridgeP(head, head, false)).light(6);
		return m;
	}

	static Model3D bow3d(String name, int[] wood, int[] gem, int pull) {
		Model3D m = new Model3D(name);
		m.display = "bow";
		double cx = 12.22, cy = 8, r = 9.22, spread = 57.8;
		Paint limb = t -> {
			if (t.flat() && Math.floorMod((int) Math.floor(t.y * TEXELS), 5) == 0 && t.i == t.w / 2) return gem[4];
			return woodP(wood).color(t);
		};
		m.arc(cx, cy, r, 180 - spread, 180, 7, 1.1, 2.1, 1.5, limb, 0);
		m.arc(cx, cy, r, 180, 180 + spread, 7, 2.1, 1.1, 1.53, limb, 0);
		m.box(3.0, 6.0, 1.9, 4.0, 1.8, gripP(LEATHER_P));
		m.box(3.0, 9.6, 2.2, 0.6, 2.0, metalP(GOLD));
		m.box(3.0, 5.8, 2.2, 0.6, 2.0, metalP(GOLD));
		m.diamond(3.0, 8.0, 1.6, 2.1, gemP(gem)).light(15);
		double sx = cx + Math.cos(Math.toRadians(180 - spread)) * r, sy = Math.sin(Math.toRadians(spread)) * r;
		m.diamond(sx, cy + sy, 1.6, 1.0, crystalP(gem)).light(15);
		m.diamond(sx, cy - sy, 1.6, 1.0, crystalP(gem)).light(15);
		double[] pullOff = {0, 1.25, 2.25, 3.25};
		if (pull == 0) m.box(sx, cy - sy, 0.25, 2 * sy, 0.25, stringP());
		else {
			double nx = sx + pullOff[pull];
			m.beam(sx, cy + sy, nx, cy, 0.25, 0.25, stringP());
			m.beam(nx, cy, sx, cy - sy, 0.25, 0.25, stringP());
			m.add(new Part(-0.6, cy - 0.15, 7.85, nx, cy + 0.15, 8.15, woodP(WOOD)));
			m.diamond(-0.6, cy, 1.7, 0.4, metalP(IRON_P));
			m.add(new Part(nx - 2.4, cy - 0.65, 7.95, nx - 0.3, cy + 0.65, 8.05, featherP(gem)));
		}
		return m;
	}

	static void weapons3d() throws IOException {
		for (Model3D m : models3d()) save(m.paint(), "item/" + m.name + "_3d");
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

	/** Bevel shading plus a dark outline around the sprite. */
	static void outline(BufferedImage img, int color) {
		bevel(img, 1.0);
		outlineRaw(img, color);
	}

	static void outlineRaw(BufferedImage img, int color) {
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

	/** Smooth palette ramp, t in 0..1 (dark .. light). */
	static int ramp(int[] pal, double t) {
		double f = clamp01(t) * (pal.length - 1);
		int i = (int) Math.floor(f);
		if (i >= pal.length - 1) return pal[pal.length - 1] & 0xFFFFFF;
		return lerp(pal[i], pal[i + 1], f - i);
	}

	/** Palette ramp snapped to half steps between the palette colours (keeps the pixel-art look crisp). */
	static int rampQ(int[] pal, double t) {
		int steps = (pal.length - 1) * 2;
		return ramp(pal, Math.round(clamp01(t) * steps) / (double) steps);
	}

	/** Deterministic per-pixel noise in 0..1. */
	static double hash(int x, int y, long seed) {
		long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (double) 0x1000000;
	}

	static double hash3(double x, double y, double z) {
		return hash((int) Math.floor(x * TEXELS + 1000), (int) Math.floor(y * TEXELS + 1000) * 31 + (int) Math.floor(z * TEXELS + 1000), 99);
	}

	/** Outline that takes its colour from the neighbouring pixel: darker below/right, a bit softer above/left. */
	static void outlineSel(BufferedImage img, double dark) {
		int w = img.getWidth(), h = img.getHeight();
		BufferedImage src = copy(img);
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			if ((src.getRGB(x, y) >>> 24) != 0) continue;
			int best = -1;
			double f = dark;
			int[][] dirs = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
			for (int k = 0; k < 4; k++) {
				int nx = x + dirs[k][0], ny = y + dirs[k][1];
				if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
				int c = src.getRGB(nx, ny);
				if ((c >>> 24) < 200) continue;
				best = c & 0xFFFFFF;
				f = k < 2 ? dark * 0.8 : dark; // shape lies up/left -> this is the shadow side
			}
			if (best >= 0) img.setRGB(x, y, argb(lerp(shadeColor(best, f), 0x0a0612, 0.35)));
		}
	}

	/** Gives a flat sprite a rounded, top-left lit bevel derived from its silhouette. */
	static void bevel(BufferedImage img, double strength) {
		int w = img.getWidth(), h = img.getHeight();
		double[][] d = new double[w][h];
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			if ((img.getRGB(x, y) >>> 24) == 0) continue;
			double best = 4;
			for (int yy = Math.max(0, y - 4); yy <= Math.min(h - 1, y + 4); yy++)
				for (int xx = Math.max(0, x - 4); xx <= Math.min(w - 1, x + 4); xx++)
					if ((img.getRGB(xx, yy) >>> 24) == 0) best = Math.min(best, Math.hypot(xx - x, yy - y));
			best = Math.min(best, Math.min(Math.min(x + 1, y + 1), Math.min(w - x, h - y)));
			d[x][y] = Math.min(best, 3);
		}
		BufferedImage src = copy(img);
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int c = src.getRGB(x, y);
			if ((c >>> 24) == 0) continue;
			double gx = (dAt(d, x + 1, y) - dAt(d, x - 1, y)) / 2, gy = (dAt(d, x, y + 1) - dAt(d, x, y - 1)) / 2;
			double lam = Math.max(-1, Math.min(1, (gx + gy) * 0.7071)) * strength;
			int rgb = c & 0xFFFFFF;
			rgb = lam > 0 ? lerp(rgb, 0xffffff, lam * 0.32) : shadeColor(rgb, 1 + lam * 0.3);
			rgb = shadeColor(rgb, 0.96 + hash(x, y, 5) * 0.08);
			img.setRGB(x, y, (c & 0xFF000000) | rgb);
		}
	}

	static double dAt(double[][] d, int x, int y) {
		if (x < 0 || y < 0 || x >= d.length || y >= d[0].length) return 0;
		return d[x][y];
	}

	/** Emboss light for a height field (tileable): positive where the surface faces the top-left. */
	static double emboss(double[][] hf, int x, int y) {
		int w = hf.length, h = hf[0].length;
		double a = hf[(x - 1 + w) % w][(y - 1 + h) % h], b = hf[(x + 1) % w][(y + 1) % h];
		return a - b;
	}

	static int shadeColor(int c, double f) {
		int r = clamp((int) (((c >> 16) & 255) * f), 0, 255);
		int g = clamp((int) (((c >> 8) & 255) * f), 0, 255);
		int b = clamp((int) ((c & 255) * f), 0, 255);
		return (r << 16) | (g << 8) | b;
	}
}
