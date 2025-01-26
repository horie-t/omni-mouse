import curses
from textwrap import dedent

def keyinput(stdscr):
    stdscr.clear()
    stdscr.addstr(dedent("""\
    終了するには 'q' を押してください。
    上左下右はそれぞれ、'w', 'a', 's', 'd' を押してください。
    左右の旋回は、'←', '→' を押してください
    """))

    while True:
        key = stdscr.getch()
        if key == ord('q'):
            break
        elif key == curses.KEY_LEFT:
            stdscr.addstr(f"Key pressed: ←\n")
        elif key == curses.KEY_RIGHT:
            stdscr.addstr(f"Key pressed: →\n")
        elif key in map(ord, ['w', 'a', 's', 'd']):
            stdscr.addstr(f"Key pressed: {chr(key)}\n")

curses.wrapper(keyinput)