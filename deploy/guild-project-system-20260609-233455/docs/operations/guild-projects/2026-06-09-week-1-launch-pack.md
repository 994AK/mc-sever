# Guild Project Week 1 Launch Pack

Created: 2026-06-09

Source plan:

- `docs/plans/2026-06-09-guild-project-daily-activity-rollout-plan.md`

Source rules:

- `docs/brainstorms/2026-06-09-guild-project-daily-activity-requirements.md`
- `docs/brainstorms/2026-06-09-guild-project-daily-activity-rules.md`

## Operator Summary

Week 1 is a pilot. Run one official project and one guild proposal slot, confirm participation manually, and reward only public recognition plus small non-combat supplies.

Fill these before publishing:

| Field | Value |
| --- | --- |
| Project wall location | `<spawn/main hub coordinates or route>` |
| Program owner | `<manager name>` |
| Official project owner | `<player or manager name>` |
| Guild proposal reviewer | `<manager name>` |
| Proposal cutoff | `2026-06-10 23:59` |
| Participation window | `2026-06-10` to `2026-06-14` |
| Review day | `2026-06-15` |
| Retrospective day | `2026-06-16` |

Reward boundary:

- Allowed: records, display,署名, temporary title候选, one light-supply bundle.
- Not allowed: free flight, flight charge, powerful gear, high-efficiency tools, WorldEdit, creative, CoreProtect, forced teleport, admin permissions, rare-resource payouts.

In-game entry:

- `/projects` shows the current project loop.
- `/daily` shows daily/weekly task prompts and lists Quests tasks.
- `/guild` shows BetterTeams guild/team commands.
- `/proposal`, `/projectrewards`, and `/projectsubmit` support project intake and review.

---

## Player Announcement

Copy this to the group chat, server announcement, or project wall.

```text
本周公会项目已开放。
项目墙位置：<坐标或路线>
负责人：<玩家名>

每周 1 个官方项目 + 1 个公会提案项目。
参与公共建设、红石材料池、交通、新手村、主城美化，都可以被记录。

奖励只给称号、署名、展示和少量补给。
不给神装、不送飞行、不加战力。

想报名：输入 /projects 看本周项目；输入 /guild 创建或加入公会；找项目负责人，或用 /helpop 内容 联系管理。
```

Short version for in-game signs:

```text
本周公会项目
1 官方 + 1 提案
只给署名/称号/少量补给
不送飞行/神装/战力
报名找负责人或 /helpop
详细看 /projects /daily /guild
```

---

## Project Wall Layout

Build a small board or booth near spawn/main hub with four zones.

| Zone | Content |
| --- | --- |
| 本周官方项目 | `W1-O1 主城项目墙 + 公会荣誉墙` |
| 本周公会提案项目 | `W1-G1 公共红石材料池 v1` unless a proposal is approved |
| 参与方式 | 找负责人 or `/helpop 内容` |
| 已完成项目 | Empty until review passes |

Recommended sign text:

```text
[本周官方]
W1-O1 项目墙
负责人: <name>
截止: 6/14
```

```text
[公会提案]
W1-G1 红石材料池
可提交替代提案
截止: 6/10
```

```text
[奖励边界]
署名/称号/展示
少量补给
不送飞行/神装
```

```text
[报名]
找负责人
或 /helpop 内容
参与会被记录
```

---

## W1-O1 Official Project Record

Project: Main Hub Project Board

| Field | Value |
| --- | --- |
| Project ID | `W1-O1` |
| Type | 建筑 / 新手 / 秩序 |
| Location | `<coordinates or route>` |
| Owner | `<official project owner>` |
| Deadline | `2026-06-14 23:59` |
| Review date | `2026-06-15` |
| Status | Not started / In progress / Submitted / Passed / Partial / Rejected |

Goal:

Create the first project wall and guild honor wall near spawn or main hub so players can see weekly projects, owners, deadlines, and participant records.

Acceptance:

- Players can find it from spawn or main hub.
- It has zones for official project, guild proposal project, completed projects, and participant list.
- It shows how to join through the owner or `/helpop 内容`.
- It reserves space for at least four weeks of project records.
- It does not block roads, portals, private claims, villages, or existing builds.

Participant draft:

| Player | Guild / Team | Contribution | Evidence | Confirmed |
| --- | --- | --- | --- | --- |
| `<player>` | `<guild/team>` | `<build/label/material/test/coordination>` | `<screenshot/coordinate/manager observed>` | Yes / No |
|  |  |  |  |  |
|  |  |  |  |  |

Reward draft:

| Recipient | Reward | Notes |
| --- | --- | --- |
| Participants | `W1 建设参与` record + one light bundle | No extra stacking |
| Core contributor | Project wall署名 + temporary title候选 | Manual recognition only |
| Guild / Team | First-week honor wall entry | Display reward |

---

## W1-G1 Guild Proposal Slot

Default project: Public Redstone Supply Pool

Use this only if no better guild proposal is approved by the cutoff.

| Field | Value |
| --- | --- |
| Project ID | `W1-G1` |
| Type | 资源 / 生电 |
| Location | `<coordinates or route>` |
| Owner | `<guild project owner>` |
| Proposal cutoff | `2026-06-10 23:59` |
| Deadline | `2026-06-14 23:59` |
| Review date | `2026-06-15` |
| Status | Waiting proposal / Default selected / In progress / Submitted / Passed / Partial / Rejected |

Goal:

Create a small public redstone supply point for future redstone builds, maintenance, and beginner learning.

Acceptance:

- The storage area is accessible and not inside a private claim.
- At least six common redstone material categories are labeled.
- Each accepted category has starter stock.
- A maintainer is named for weekly checks.
- Signs state that supplies are for public projects first and should not be emptied.

Starter categories:

| Category | Starter Target | Ready |
| --- | --- | --- |
| Redstone dust | Small starter stock | Yes / No |
| Redstone torch | Small starter stock | Yes / No |
| Repeater | Small starter stock | Yes / No |
| Comparator | Small starter stock | Yes / No |
| Hopper | Small starter stock | Yes / No |
| Piston / sticky piston | Small starter stock | Yes / No |
| Observer | Optional starter stock | Yes / No |
| Iron / quartz / wood support materials | Optional starter stock | Yes / No |

Participant draft:

| Player | Guild / Team | Contribution | Evidence | Confirmed |
| --- | --- | --- | --- | --- |
| `<player>` | `<guild/team>` | `<donated/sorted/labeled/built/tested>` | `<screenshot/coordinate/material note>` | Yes / No |
|  |  |  |  |  |
|  |  |  |  |  |

Reward draft:

| Recipient | Reward | Notes |
| --- | --- | --- |
| Participants | `W1 红石材料池参与` record + one light bundle | No raw amount contest |
| Maintainer | Material pool署名 + proposal priority候选 | Manual recognition only |
| Guild / Team | Week 1 guild project record | Display reward |

---

## Guild Proposal Intake

Use this before selecting `W1-G1`.

```text
【公会项目提案】

公会 / 小队名：
提案人：
项目名：
项目类型：
项目地点：
预计参与人数：
预计完成时间：

这个项目对服务器有什么公共价值：

需要管理协助：

完成后如何验收：
1.
2.
3.
```

Approval checklist:

| Check | Pass |
| --- | --- |
| Serves public construction, resources, redstone, building, new players, or order | Yes / No |
| Not only a private base project | Yes / No |
| Does not require flight, powerful gear, creative, WorldEdit, CoreProtect, or admin permissions | Yes / No |
| Location does not conflict with private claims, roads, portals, villages, or other builds | Yes / No |
| Has a named project lead | Yes / No |
| Has visible completion criteria | Yes / No |

Decision:

- Approved as `W1-G1`
- Rejected as private project
- Needs changes before approval

---

## Review Checklist

Use this on review day for each project.

| Check | `W1-O1` | `W1-G1` |
| --- | --- | --- |
| Project ID and owner are clear | Pass / Fix | Pass / Fix |
| Public value is visible | Pass / Partial / Reject | Pass / Partial / Reject |
| Location is safe | Pass / Fix | Pass / Fix |
| Published acceptance criteria are met | Pass / Partial / Reject | Pass / Partial / Reject |
| Participant list is plausible | Pass /补证 | Pass /补证 |
| Evidence exists | Pass /补证 | Pass /补证 |
| Rewards stay inside boundary | Pass / Adjust | Pass / Adjust |
| Display or announcement updated | Done / Pending | Done / Pending |

Review outcome:

| Project | Outcome | Notes |
| --- | --- | --- |
| `W1-O1` | Pass / Partial / Reject |  |
| `W1-G1` | Pass / Partial / Reject |  |

---

## Light Reward Bundles

Pick one bundle per confirmed participant. Do not stack multiple bundles for the same project unless management approves a core-contributor exception.

| Bundle | Contents | Use |
| --- | --- | --- |
| Builder bundle | 1-3 stacks ordinary building blocks + 32 torches | Project wall, roads, public builds |
| Runner bundle | 16-32 food + 8-16 normal fireworks | Travel and participation feedback |
| Maintainer bundle | 8-16 XP bottles +纪念书 or命名纸 | Redstone testing, maintenance, project lead |

Forbidden reward requests:

| Request | Decision |
| --- | --- |
| Free `/fly` or flight charge | Reject |
| Diamond / netherite gear | Reject |
| High-level enchanted tools | Reject |
| WorldEdit / creative / CoreProtect / admin permission | Reject |
| Large rare-resource payout | Reject |
| Reward based only on idling online | Reject |

---

## Display Update Text

Use after a project passes review.

```text
【本周完成项目】
<项目编号> <项目名>

参与公会 / 小队：
<guild or team names>

参与玩家：
<player names>

奖励：
署名 / 称号候选 / 少量补给
```

Short sign version:

```text
[已完成]
<W1-O1/W1-G1>
<项目名>
参与: <names>
```

---

## Week 1 Retrospective

Fill this after review.

| Metric | Target | Actual | Decision |
| --- | --- | --- | --- |
| Unique participants | 4+ |  | Continue / Adjust |
| Teams or guilds involved | 1+ |  | Continue / Adjust |
| Completed public projects | 1+ |  | Continue / Adjust |
| Management review time | Under 30 minutes per project |  | Continue / Reduce |
| Reward disputes | 0-1 |  | Continue / Tighten rules |
| Boundary violations | 0 |  | Continue / Tighten rules |

Retrospective notes:

```text
Did players understand where to find projects?

Did at least one project produce visible server value?

Did manual review take less than one management session?

Did any reward feel too strong or too weak?

Did public recognition motivate people more than item rewards?

Week 2 decision:
```

Week 2 gate:

| Condition | Move |
| --- | --- |
| Good participation, low management load | Continue with 1 official + 1 proposal |
| Good participation, high management load | Require stronger project lead records |
| Low participation, low confusion | Use easier projects and stronger visibility |
| Low participation, high confusion | Pause proposal slot and run one official tutorial project |
| Reward disputes or boundary pressure | Tighten reward language before next launch |

Do not add plugin automation until the manual loop produces at least two successful weekly cycles.
