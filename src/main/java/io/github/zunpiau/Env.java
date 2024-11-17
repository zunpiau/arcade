package io.github.zunpiau;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import io.github.kingpulse.XFacade;
import io.github.kingpulse.structs.xdo_search_t;
import io.github.kingpulse.structs.xdo_t;
import io.github.kingpulse.xdotool;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
            System.err.println("仅支持Windows和Linux平台");
            System.exit(4);
            throw new RuntimeException();
        }
    }

    private static final Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    byte[] capture(int x, int y, int w, int h) {
        BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, w, h));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "bmp", baos);
        return baos.toByteArray();
    }

    abstract void press(Character key);

    abstract void click(int x, int y);

    private static class LinuxX11 extends Env {

        private final xdotool lib = xdotool.loadLib();
        private final xdo_t xdo;

        private final X11.Window window;
        private final Point windowPosition;

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
        }

        @Override
        byte[] capture(int x, int y, int w, int h) {
            return super.capture(windowPosition.x + x, windowPosition.y + y, w, h);
        }

        @Override
        @SneakyThrows
        void press(Character key) {
            lib.xdo_send_keysequence_window(xdo, window, String.valueOf(key), 12000);
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

        private final WinDef.HWND window;
        private final Point windowPosition;

        public Win() {
            String windowTitle = System.getProperty(WINDOW_TITLE_KEY, "NIKKE");
            window = user32.FindWindow("UnityWndClass", windowTitle);
            if (window == null) {
                System.err.println("根据窗口标题[" + windowTitle + "]找不到进程，请确认是否启动");
                System.exit(1);
            }
            char[] className = new char[128];
            user32.GetClassName(window, className, className.length);
            System.out.println(className);
            WinDef.RECT rect = new WinDef.RECT();
            user32.GetWindowRect(window, rect);
            System.out.println(rect);
            WinUser.WINDOWINFO windowinfo = new WinUser.WINDOWINFO();
            user32.GetWindowInfo(window, windowinfo);
            System.out.println(windowinfo);
            IntByReference processId = new IntByReference();
            user32.GetWindowThreadProcessId(window, processId);
            System.out.println(processId.getValue());

            long pid = Kernel32.INSTANCE.GetCurrentThreadId();
            System.out.println(pid);

            System.out.println(user32.AttachThreadInput(new WinDef.DWORD(processId.getValue()), new WinDef.DWORD(pid), true));
            user32.SetForegroundWindow(window);
            user32.SetFocus(window);
            System.out.println(user32.AttachThreadInput(new WinDef.DWORD(processId.getValue()), new WinDef.DWORD(pid), false));

//            int width = rect.right - rect.left;
//            int height = rect.bottom - rect.top;
//            if (width != 1920 || height != 1080) {
//                System.err.println("仅支持1080P分辨率");
//                System.exit(3);
//            }
            windowPosition = new Point(rect.left, rect.top);
        }

        @Override
        byte[] capture(int x, int y, int w, int h) {
            return super.capture(windowPosition.x + x, windowPosition.y + y, w, h);
        }

        @Override
        void press(Character key) {
            WinUser.INPUT input = new WinUser.INPUT();
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType("ki");
            input.input.ki.wScan = new WinDef.WORD(0);
            input.input.ki.time = new WinDef.DWORD(0);
            input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
            input.input.ki.wVk = new WinDef.WORD(key);
            input.input.ki.dwFlags = new WinDef.DWORD(0);
            user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
            input.input.ki.wVk = new WinDef.WORD(key);
            input.input.ki.dwFlags = new WinDef.DWORD(2);
            user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
        }

        @Override
        void click(int x, int y) {
            WinUser.INPUT input = new WinUser.INPUT();
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType("mi");
            input.input.mi.dx = new WinDef.LONG(x);
            input.input.mi.dy = new WinDef.LONG(y);
            input.input.mi.dwFlags = new WinDef.DWORD(2);
            user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
            input.input.mi.dwFlags = new WinDef.DWORD(4);
            user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
        }
    }

}
