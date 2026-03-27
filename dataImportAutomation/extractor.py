from itertools import accumulate
import pandas as pd
import re
from rich.progress import track

import sys

try:
    input_filename = sys.argv[1]
except IndexError:
    input_filename = "round-4-Maharashtra.csv"


df_raw = pd.read_csv(input_filename)
lines = df_raw[df_raw.columns[0]].to_list()

regional_reservation_types = [
    "Home University Seats Allotted to Home University Candidates",
    "Home University Seats Allotted to Other Than Home University Candidates",
    "Other Than Home University Seats Allotted to Home University Candidates"
    "Other Than Home University Seats Allotted to Other Than Home University Candidates",
    "State Level",
]

rows = []
row = {}

f = open("rawranklines.txt", "w")


#
# def extract_table_data(category_line, rank_line):
#     word_begin = 0
#     word_end = 0
#     i = 0
#     word = ""
#     while i < len(category_line):
#         if category_line[i].isnumeric():
#             word_begin = i
#             while category_line[i] != " ":
#                 word_end = i
#                 i += 1
#             word_len = word_end - word_begin
#             search_index = word_begin + word_len / 2
#
#             rank_parts = re.findall(r"\s+|\S+", rank_line)
#             parts_indices = list(accumulate(map(len, parts)))
#             word = next((filter(lambda x: x >= search_index, parts_indices)), None)
#     i += 1
#
#     yield (category_line[word_begin:word_end], word)


# AI GEN CODE START ----------


def parse_lines(category_line, rank_line, percentile_line):
    results = []
    i = 0
    while i < len(category_line):
        if category_line[i].isspace():
            i += 1
            continue

        # find the start and end of the current word
        start = i
        while i < len(category_line) and not category_line[i].isspace():
            i += 1
        end = i

        cat = category_line[start:end].strip()

        # extract corresponding word from rank_line at mid

        for j in range(start, end):
            rank_word = extract_word_at(rank_line, j)
            if rank_word:
                # print(rank_word)
                break

        for j in range(start, end):
            perc_word = extract_word_at(percentile_line, j)
            if perc_word:
                # print(perc_word)
                break

        # try:
        #     rank_word = [
        #         *filter(
        #             bool, [extract_word_at(rank_line, i) for i in range(start, end)]
        #         )
        #     ][0]
        #     perc_word = [
        #         *filter(
        #             bool,
        #             [extract_word_at(percentile_line, i) for i in range(start, end)],
        #         )
        #     ][0]
        #
        #     results.append((cat, rank_word, perc_word))
        #
        # except IndexError:
        #     print("Couldn't extract {cat} from following lines:")
        #     print(category_line, rank_line, percentile_line, sep="\n")
        # print(category_line, rank_line, percentile_line, sep="\n")
        if rank_word and perc_word:
            results.append((cat, rank_word, perc_word))

    return results


def extract_word_at(line, index):
    """Return the word from line that covers index, or None if no such word."""
    if index >= len(line):
        return None
    # go left to find start
    start = index
    while start > 0 and not line[start - 1].isspace():
        start -= 1
    # go right to find end
    end = index
    while end < len(line) and not line[end].isspace():
        end += 1
    word = line[start:end].strip()
    return word if word else None


# AI GEN CODE END --------------


category_line = ""
rank_line = ""
percentile_line = ""

entries_count = 0

for i, line in enumerate(track(lines)):
    if type(line) is not str:
        line = str(line)

    college_match = re.match(r"^(\d{5})\s-\s(.+)", str(line))
    course_match = re.match(r"^(\d{10})\s-\s(.+)", str(line))
    status_match = re.match(r"^Status: (.+?) Home University : (.+)$", str(line))

    if line == "nan":
        continue

    elif college_match:
        row["college_code"] = college_match.group(1)
        row["college_name"] = college_match.group(2)

    elif course_match:
        row["course_code"] = course_match.group(1)
        row["course_name"] = course_match.group(2)

    elif status_match:
        row["course_status"] = status_match.group(1)
        row["course_university"] = status_match.group(2)

    elif line in regional_reservation_types:
        row["regional_reservation"] = line

    elif line.startswith("Stage"):
        # row['category_reservation'] = line.split()[1:]
        current_entries_count = len(line.split()[1:])
        entries_count += current_entries_count
        category_line = line.removeprefix("Stage ")

    elif line.startswith("I"):  # also works for II and III
        rank_line = line

        for _ in range(3):
            rank_line = rank_line.removeprefix("I")

        row["last_cap_round"] = len(line.split()[0])  # easy hack
        # print(rank_line)
        percentile_line = lines[i + 1]

        f.write(category_line + "\n")
        f.write(rank_line + "\n")
        f.write(percentile_line + "\n")
        parsed_lines = parse_lines(category_line, rank_line, percentile_line)

        if len(parsed_lines) != current_entries_count:
            print(
                f"================\n",
                f"College name: {row['college_name']}\n",
                f"Course_name: {row['course_name']}",
                f"Course_type: {row['regional_reservation']}",
                f"parsed_lines: \n {'\n'.join(map(str, parsed_lines))}\n",
                f"{len(parsed_lines)} of {current_entries_count}\n",
            )

        for category_reservation, last_rank, cutoff_percentile in parsed_lines:
            row["category_reservation"] = category_reservation
            row["last_rank"] = last_rank
            row["cutoff_percentile"] = cutoff_percentile
            rows.append(row.copy())

f.close()

df = pd.DataFrame(rows)
df.to_csv(f"extracted_{input_filename}")
print(f"Scraped {len(rows)} of {entries_count}")
