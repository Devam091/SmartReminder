import sys
import mysql.connector
from datetime import datetime, timedelta
import random
import re
import io

# Make stdout UTF-8 (helps avoid Windows cp1252 errors when printing emojis)
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)

# Candidate free-time activities
CANDIDATES = [
    "Read a book", "Power nap", "Go for a walk", "Meditate",
    "Tidy your room", "Listen to a podcast", "Journal", "Light stretching",
    "Gaming", "Family Time"
]

# --- Suggestion Engine ---
ROUTINE_ORDER = [
    "Wake up", "Breakfast", "Bath", "Commute", "Work/Study", "Lunch",
    "Short walk", "Tea/Coffee break", "Exercise", "Dinner", "Relax",
    "Read a book", "Plan tomorrow", "Sleep"
]

GENERIC_ACTIVITIES = [
    "Read a book", "Power nap", "Go for a walk", "Meditate",
    "Tidy your room", "Listen to a podcast", "Journal", "Light stretching"
]

# connect to your XAMPP MySQL
try:
    db = mysql.connector.connect(
        host="localhost",
        user="root",
        password="",
        database="taskmanager"
    )
    cursor = db.cursor(dictionary=True)
except mysql.connector.Error as err:
    print(f"⚠ Database connection failed: {err}", flush=True)
    sys.exit(1)

# ----------- ARGUMENT HANDLING -----------
args = sys.argv[1:]
special_flag = None
if args and args[0].startswith("--"):
    special_flag = args[0]
    args = args[1:]

# Join rest into user input (for chat use). Keep original case for some parsing needs:
user_message = " ".join(args).strip()
user_message_lower = user_message.lower()

# synonym mapping (all lowercase for easier compare)
synonyms = {
    "improve": ["improve", "organize", "optimize"],
    "free": ["free", "available"],
    "suggest": ["suggest", "recommend"],
    "ok": ["okay", "okai", "ok"],
    "greet": ["heyy", "yoo", "hii", "hi", "hello", "hey"]
}

def matches(word_list):
    """Case-insensitive substring matcher."""
    for w in word_list:
        if w.lower() in user_message_lower:
            return True
    return False

def pick_new(tasks):
    have = {t["description"].lower() for t in tasks if t.get("description")}
    pool = [c for c in CANDIDATES if c.lower() not in have]
    source = pool if pool else CANDIDATES
    return random.choice(source)

# ---------- AI PARSER (light) ----------
def parse_task(message: str, base_date: datetime = None):
    """
    Parse a time + duration from a free-form message.
    If base_date is provided it will be used as the date for the parsed time.
    Returns dict with description, start_time (datetime or None), end_time (datetime or None).
    """
    desc = message.strip()
    start_time = None
    duration_minutes = 60  # default 1 hour

    # find "for X minutes/hours"
    dmatch = re.search(r'for\s+(\d+)\s*(minute|minutes|hour|hours)', message, flags=re.IGNORECASE)
    if dmatch:
        val = int(dmatch.group(1))
        unit = dmatch.group(2).lower()
        duration_minutes = val if "minute" in unit else val * 60

    # find time like "5 pm", "17:30", "10"
    match = re.search(r'(\d{1,2})(?::(\d{2}))?\s*(am|pm)?', message, flags=re.IGNORECASE)
    if match:
        hour = int(match.group(1))
        minute = int(match.group(2)) if match.group(2) else 0
        ampm = match.group(3)
        if ampm:
            ampm = ampm.lower()
            if ampm == "pm" and hour != 12:
                hour += 12
            if ampm == "am" and hour == 12:
                hour = 0

        # anchor to provided base_date if available, otherwise today
        if base_date and isinstance(base_date, datetime):
            day = base_date
        else:
            day = datetime.now()

        start_time = day.replace(hour=hour, minute=minute, second=0, microsecond=0)

        # if start_time is in the past relative to "day" (and no explicit date provided),
        # we keep it anchored to the provided date. This function does NOT auto-shift dates.
        # Java UI/TaskParser should supply the intended date when adding tasks.

    end_time = (start_time + timedelta(minutes=duration_minutes)) if start_time else None
    return {"description": desc, "start_time": start_time, "end_time": end_time}

# ---------- CONFLICT / DUPLICATE HELPERS ----------
def overlaps(a_start, a_end, b_start, b_end):
    """True if the two intervals overlap (strict overlap)."""
    return (a_start < b_end) and (b_start < a_end)

def conflicts(new_task, existing):
    overlaps_list = []
    for t in existing:
        # ensure both tasks have datetimes
        if not t.get("start_time") or not t.get("end_time"):
            continue
        if overlaps(new_task["start_time"], new_task["end_time"], t["start_time"], t["end_time"]):
            overlaps_list.append(t)
    return overlaps_list

def conflict_message(new_task, existing):
    overlaps_list = conflicts(new_task, existing)
    if not overlaps_list:
        return None
    overlaps_list.sort(key=lambda t: t["start_time"])
    first = overlaps_list[0]
    latest_end = max(t["end_time"] for t in overlaps_list)
    duration_minutes = int((new_task["end_time"] - new_task["start_time"]).total_seconds() / 60)
    proposed_start = latest_end
    proposed_end = proposed_start + timedelta(minutes=duration_minutes)
    return (f"⚠ Conflict detected with {len(overlaps_list)} task(s). "
            f"Example: \"{first['description']}\" overlaps. "
            f"Try scheduling \"{new_task['description']}\" from "
            f"{proposed_start.strftime('%I:%M %p')} to {proposed_end.strftime('%I:%M %p')}.")

def duplicate_activity_message(new_task, existing):
    for t in existing:
        if t.get("description") and new_task.get("description") and t["description"].lower() == new_task["description"].lower():
            # if times differ it's a duplicate activity
            if t.get("start_time") != new_task.get("start_time"):
                return (f"ℹ You already have \"{new_task['description']}\" earlier at "
                        f"{t['start_time'].strftime('%I:%M %p')}–{t['end_time'].strftime('%I:%M %p')}. "
                        f"Try something new in this period.")
    return None

# ---------- IMPROVE SCHEDULE ----------
def improve_schedule(db_tasks):
    """
    db_tasks: list of dict rows from DB with start_time/end_time datetimes.
    Returns a string with suggestions (or OK message).
    """
    suggestions = []
    # 1) Detect overlaps among DB tasks
    n = len(db_tasks)
    for i in range(n):
        a = db_tasks[i]
        if not a.get("start_time") or not a.get("end_time"):
            continue
        for j in range(i+1, n):
            b = db_tasks[j]
            if not b.get("start_time") or not b.get("end_time"):
                continue
            if overlaps(a["start_time"], a["end_time"], b["start_time"], b["end_time"]):
                # determine earlier / later
                if a["start_time"] <= b["start_time"]:
                    earlier, later = a, b
                else:
                    earlier, later = b, a
                suggestions.append(
                    f"⚠ Conflict: \"{later['description']}\" overlaps \"{earlier['description']}\". "
                    f"Consider moving \"{later['description']}\" to start after {earlier['end_time'].strftime('%I:%M %p')} "
                    f"or split it into smaller parts."
                )

    # 2) Detect very long or unhealthy tasks
    for t in db_tasks:
        if not t.get("start_time") or not t.get("end_time"):
            continue
        desc = (t.get("description") or "").lower()
        duration_hours = (t["end_time"] - t["start_time"]).total_seconds() / 3600.0
        if any(k in desc for k in ("game", "netflix", "movie")):
            if duration_hours >= 5:
                suggestions.append(
                    f"⚠️ Your task \"{t['description']}\" is {int(duration_hours)} hours long. That's long — try reducing it to 2–3 hours."
                )
        if duration_hours >= 10:
            suggestions.append(
                f"⚠️ Task \"{t['description']}\" is very long ({int(duration_hours)} hours). Consider splitting it or adding breaks."
            )

    if not suggestions:
        return "✅ Your schedule looks balanced. No major improvements needed."
    # de-duplicate suggestions and join
    seen = []
    out = []
    for s in suggestions:
        if s not in seen:
            seen.append(s)
            out.append(s)
    return "\n".join(out)

# ----------------- UTILS -----------------
def parse_iso_datetime(s):
    """Try to robustly parse datetime strings Java might send (T or space separated)."""
    if s is None:
        return None
    if isinstance(s, datetime):
        return s
    s = s.strip()
    try:
        return datetime.fromisoformat(s)
    except Exception:
        # try common formats
        fmts = ["%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M", "%Y-%m-%dT%H:%M"]
        for f in fmts:
            try:
                return datetime.strptime(s, f)
            except Exception:
                continue
    # fallback: return None
    return None

# ---------------- MAIN FLOW ----------------
response = None

# ------------- special flag: conflict check from Java -------------
if special_flag == "--conflict":
    # Expected args: description, start_iso, end_iso
    # Java used to call: python bot.py --conflict desc start_iso end_iso
    desc = args[0] if len(args) >= 1 else ""
    start_iso = args[1] if len(args) >= 2 else None
    end_iso = args[2] if len(args) >= 3 else None

    new_task = {
        "description": desc,
        "start_time": parse_iso_datetime(start_iso),
        "end_time": parse_iso_datetime(end_iso)
    }

    # fetch tasks from DB
    cursor.execute("SELECT * FROM tasks ORDER BY start_time")
    db_tasks = cursor.fetchall()

    # ensure DB rows have datetime objects (they should already)
    # check conflicts
    msg = conflict_message(new_task, db_tasks)
    if msg:
        print(msg, flush=True)
    else:
        print("OK", flush=True)
    sys.exit(0)

# ------------- normal chat mode -------------
# We will check for intent in the following order:
# 1) improve (so "heyy can you improve..." goes to improve)
# 2) free
# 3) suggest
# 4) greet
# 5) ok
# 6) fallback

if matches(synonyms["improve"]):
    cursor.execute("SELECT * FROM tasks ORDER BY start_time")
    db_tasks = cursor.fetchall()
    response = improve_schedule(db_tasks)

elif matches(synonyms["free"]):
    cursor.execute("SELECT * FROM tasks ORDER BY start_time")
    tasks = cursor.fetchall()
    if not tasks:
        response = "✅ You have no tasks scheduled, the whole day is free!"
    else:
        free_slots = []
        last_end = None
        for task in tasks:
            start = task["start_time"]
            end = task["end_time"]
            if last_end and start > last_end:
                free_slots.append(f"🕒 Free from {last_end.strftime('%I:%M %p')} to {start.strftime('%I:%M %p')}")
            last_end = end
        if last_end:
            free_slots.append(f"🕒 Free after {last_end.strftime('%I:%M %p')}")
        response = "\n".join(free_slots) if free_slots else "⚠ No free time, tasks are back-to-back!"

elif matches(synonyms["suggest"]):
    cursor.execute("SELECT * FROM tasks ORDER BY start_time")
    tasks = cursor.fetchall()
    if not tasks:
        activity = random.choice(CANDIDATES)
        activity1 = random.choice(CANDIDATES)
        response = f"💡 How about starting with: {activity} or {activity1}?"
    else:
        now = datetime.now()
        day_end = now.replace(hour=23, minute=59, second=0, microsecond=0)
        cursor_time = now
        for task in tasks + [{"start_time": day_end, "end_time": day_end}]:
            start = task["start_time"]
            if cursor_time < start:
                gap = (start - cursor_time).total_seconds() / 60
                if gap >= 30:
                    slot_end = cursor_time + timedelta(minutes=30)
                    new_activity = pick_new(tasks)
                    new_activity1 = pick_new(tasks)
                    response = f"💡 From {cursor_time.strftime('%I:%M %p')} to {slot_end.strftime('%I:%M %p')}: {new_activity} or {new_activity1}"
                    break
            if task["end_time"] and task["end_time"] > cursor_time:
                cursor_time = task["end_time"]
        if not response:
            response = "⚠ No 30-min free slot left today for new activities."

elif matches(synonyms["greet"]):
    response = "Heyy ! What's today's plan ? or stuck ? No worries...I am here to help ! ;)"

elif matches(synonyms["ok"]):
    response = "Sure, let me know if you want to know anything more."

else:
    response = "Hmm, I didn't get that. Try asking about free time, improvements, or suggestions."

print(response, flush=True)
