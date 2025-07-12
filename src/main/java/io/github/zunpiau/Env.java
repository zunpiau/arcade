package io.github.zunpiau;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import io.github.kingpulse.XFacade;
import io.github.kingpulse.structs.xdo_search_t;
import io.github.kingpulse.structs.xdo_t;
import io.github.kingpulse.xdotool;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

public abstract class Env {

    @Getter
    private static final Env instance;

    final String WINDOW_TITLE_KEY = "arcade.title";

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            String de = System.getenv("XDG_SESSION_TYPE");
            if (!"x11".equalsIgnoreCase(de)) {
                System.err.println("Linux平台仅支持X11桌面环境");
                System.exit(4);
                throw new RuntimeException();
            } else {
                instance = new LinuxX11();
            }
        } else if (os.contains("windows")) {
            instance = new Win();
        } else {
            System.err.println(I18n.t("env_windows_or_linux"));
            System.exit(4);
            throw new RuntimeException();
        }
    }

    abstract byte[] capture(int x, int y, int w, int h);

    abstract void press(String keys);

    abstract void click(int x, int y);

    private static class LinuxX11 extends Env {

        private final xdotool lib = xdotool.loadLib();
        private final xdo_t xdo;
        private final Robot robot;

        private final X11.Window window;
        private final Point windowPosition;

        @SneakyThrows
        public LinuxX11() {
            X11 x11 = X11.INSTANCE;
            X11.Display display = x11.XOpenDisplay(null);

            xdo_search_t search = new xdo_search_t();
            search.winname = System.getProperty(WINDOW_TITLE_KEY, "- Moonlight");
            search.searchmask = xdo_search_t.searchmask_SEARCH_NAME;
            xdo = lib.xdo_new_with_opened_display(display, null, 0);
            XFacade fac = new XFacade();
            X11.Window[] windows = fac.searchWindows(xdo, search);
            if (windows.length == 0) {
                System.err.println("根据窗口标题[" + search.winname + "]找不到进程，请确认是否启动。或通过启动参数 -D" + WINDOW_TITLE_KEY + "=<value> 指定标题");
                System.exit(1);
            }
            if (windows.length > 1) {
                System.err.println("根据窗口标题[" + search.winname + "]找到多个进程，通过启动参数 -D" + WINDOW_TITLE_KEY + "=<value> 指定标题");
                System.exit(2);
            }
            window = windows[0];

            X11.XWindowAttributes attributes = new X11.XWindowAttributes();
            x11.XGetWindowAttributes(display, window, attributes);
            IntByReference x = new IntByReference();
            IntByReference y = new IntByReference();
            x11.XTranslateCoordinates(display, window, attributes.root, 0, 0, x, y, new X11.WindowByReference());
            if (attributes.width != 1920 || attributes.height != 1080) {
                System.err.println("仅支持1080P分辨率");
                System.exit(3);
            }
            windowPosition = new Point(x.getValue(), y.getValue());
            robot = new Robot();
        }

        @Override
        @SneakyThrows
        byte[] capture(int x, int y, int w, int h) {
            BufferedImage image = robot.createScreenCapture(new Rectangle(windowPosition.x + x, windowPosition.y + y, w, h));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "bmp", baos);
            return baos.toByteArray();
        }

        @Override
        void press(String keys) {
            if (keys.length() == 1) {
                lib.xdo_send_keysequence_window(xdo, window, keys, 2000);
            } else {
                lib.xdo_enter_text_window(xdo, window, keys, 1000);
            }
        }

        @Override
        @SneakyThrows
        void click(int x, int y) {
            lib.xdo_move_mouse_relative_to_window(xdo, window, x, y);
            TimeUnit.MILLISECONDS.sleep(1);
            lib.xdo_click_window(xdo, window, 1);
        }
    }

    private static class Win extends Env {

        private final User32 user32 = User32.INSTANCE;
        private final User32Ext user32Ext = User32Ext.INSTANCE;
        private final GDI32 gdi32 = GDI32.INSTANCE;

        private final WinDef.HWND window;

        @SneakyThrows
        public Win() {
            if (!Advapi32Util.isCurrentProcessElevated()) {
                System.err.println(I18n.t("env_admin"));
                System.exit(2);
            }
            String windowTitle = System.getProperty(WINDOW_TITLE_KEY, "NIKKE");
            window = user32.FindWindow("UnityWndClass", windowTitle);
            if (window == null) {
                System.err.printf(I18n.t("env_window_not_found"), windowTitle);
                System.exit(1);
            }
            WinDef.RECT rect = new WinDef.RECT();
            user32.GetClientRect(window, rect);
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;
            if (width != 1920 || height != 1080) {
                System.out.println(I18n.t("env_resolution_hints"));
                System.out.println(I18n.t("env_try_scale"));
                user32.MoveWindow(window, 0, 0, 1920, 1080, true);
                TimeUnit.MILLISECONDS.sleep(10);
                user32.GetClientRect(window, rect);
                user32.MoveWindow(window, 0, 0, 1920 + 1920 - rect.right, 1080 + 1080 - rect.bottom, true);
                user32.GetClientRect(window, rect);
                System.out.printf(I18n.t("env_current_resolution"), rect.right, rect.bottom);
            }

            System.out.println(I18n.t("env_window_bottom"));
            TimeUnit.SECONDS.sleep(3);
            IntByReference processId = new IntByReference();
            user32.GetWindowThreadProcessId(window, processId);
            long pid = Kernel32.INSTANCE.GetCurrentThreadId();
            user32.AttachThreadInput(new WinDef.DWORD(processId.getValue()), new WinDef.DWORD(pid), true);
            user32.SetForegroundWindow(window);
            user32.SetFocus(window);
            user32.AttachThreadInput(new WinDef.DWORD(processId.getValue()), new WinDef.DWORD(pid), false);
        }

        @SneakyThrows
        public byte[] capture(int x, int y, int w, int h) {
            WinDef.HDC hScreenDC = user32.GetDC(null);
            WinDef.HDC hMemoryDC = gdi32.CreateCompatibleDC(hScreenDC);

            WinDef.HBITMAP hBitmap = gdi32.CreateCompatibleBitmap(hScreenDC, w, h);
            gdi32.SelectObject(hMemoryDC, hBitmap);
            WinDef.POINT point = new WinDef.POINT(x, y);
            user32Ext.ClientToScreen(window, point);
            gdi32.BitBlt(hMemoryDC, 0, 0, w, h, hScreenDC, point.x, point.y, GDI32.SRCCOPY);

            WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
            bmi.bmiHeader.biWidth = w;
            bmi.bmiHeader.biHeight = -h;
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = 32;
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

            Memory buffer = new Memory(w * h * 4L);
            gdi32.GetDIBits(hScreenDC, hBitmap, 0, h, buffer, bmi, WinGDI.DIB_RGB_COLORS);
            int bufferSize = w * h;
            DataBuffer dataBuffer = new DataBufferInt(buffer.getIntArray(0, bufferSize), bufferSize);
            DirectColorModel SCREENSHOT_COLOR_MODEL = new DirectColorModel(24, 0x00FF0000, 0xFF00, 0xFF);
            WritableRaster raster = Raster.createPackedRaster(dataBuffer, w, h, w,
                    new int[]{
                            SCREENSHOT_COLOR_MODEL.getRedMask(),
                            SCREENSHOT_COLOR_MODEL.getGreenMask(),
                            SCREENSHOT_COLOR_MODEL.getBlueMask()
                    }, null);
            BufferedImage image = new BufferedImage(SCREENSHOT_COLOR_MODEL, raster, false, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "bmp", baos);
            byte[] bytes = baos.toByteArray();
            gdi32.DeleteObject(hBitmap);
            gdi32.DeleteDC(hMemoryDC);
            user32.ReleaseDC(null, hScreenDC);
            return bytes;
        }

        void press(Character key) {
            WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(2);
            fillInput(inputs[0], key, 0);
            fillInput(inputs[1], key, WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);
            user32.SendInput(new WinDef.DWORD(2), inputs, inputs[0].size());
        }

        @Override
        void press(String keys) {
            keys.chars().forEach(c -> press((char) c));
        }

        @Override
        void click(int x, int y) {
            WinDef.POINT point = new WinDef.POINT(x, y);
            user32Ext.ClientToScreen(window, point);
            user32.SetCursorPos(point.x, point.y);
            WinUser.INPUT input = new WinUser.INPUT();
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType("mi");
            input.input.mi.dx = new WinDef.LONG(point.x);
            input.input.mi.dy = new WinDef.LONG(point.y);
            input.input.mi.dwFlags = new WinDef.DWORD(2);
            user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
            input.input.mi.dwFlags = new WinDef.DWORD(4);
            user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
        }

        private static void fillInput(WinUser.INPUT input, Character key, int flag) {
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType("ki");
            input.input.ki.wScan = new WinDef.WORD(0);
            input.input.ki.time = new WinDef.DWORD(0);
            input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
            input.input.ki.wVk = new WinDef.WORD(key);
            input.input.ki.dwFlags = new WinDef.DWORD(flag);
        }
    }

    interface User32Ext extends StdCallLibrary {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean ClientToScreen(WinDef.HWND hWnd, WinDef.POINT lpPoint);
    }

}
