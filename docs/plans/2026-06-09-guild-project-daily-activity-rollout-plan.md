# Guild Project Daily Activity Rollout Plan

Created: 2026-06-09
Execution: knowledge-work

Origin:

- `docs/brainstorms/2026-06-09-guild-project-daily-activity-requirements.md`
- `docs/brainstorms/2026-06-09-guild-project-daily-activity-rules.md`

## Summary

This plan rolls out the guild project daily activity system as a one-week operational pilot. The pilot uses BetterTeams for light guild identity, Quests for daily/weekly entry tasks, CMI/CommandGUI for in-game navigation, manual participation confirmation, visible recognition, and non-combat rewards only.

---

## Goals

- Run the first weekly cycle with a small plugin-assisted in-game surface.
- Prove that players understand how to join public projects.
- Prove that management can confirm participation without automatic contribution tracking.
- Keep all rewards inside the pure-survival boundary: no combat growth, no free flight, no powerful tools, no admin permissions.
- Produce enough records to decide whether week 2 should keep, shrink, or expand the plugin-assisted flow.

---

## Non-Goals

- Do not install heavy town, RPG, profession, combat, economy, or reward-shop systems such as Towny, mcMMO, AuraSkills, Jobs Reborn, or EliteMobs.
- Do not create an automatic guild contribution score.
- Do not create a reward shop, point economy, or infinite grind loop.
- Do not give flight charge, `/fly`, WorldEdit, creative mode, CoreProtect, high-efficiency tools, or rare-resource payouts as activity rewards.
- Do not try to solve long-term guild structure before proving the first weekly project loop.

---

## Roles

| Role | Owner | Responsibility |
| --- | --- | --- |
| Program owner | 管理 | Owns weekly cadence, approves projects, resolves disputes |
| Official project owner | 管理 or trusted builder | Publishes and verifies `W1-O1` |
| Guild proposal reviewer | 管理 | Reviews guild proposals and picks `W1-G1` |
| Project lead | 公会成员 or 管理 | Tracks participants, coordinates work, submits records |
| Participants | 普通玩家 | Join projects, contribute work, provide evidence when needed |

If there are no stable guilds yet, a temporary team of at least 2 players can act as a pilot guild for week 1 only.

---

## In-Game Surfaces

| Surface | File / Plugin | Player Command | Purpose |
| --- | --- | --- | --- |
| Project overview | CMI CustomText | `/projects` | Shows the weekly official project, guild proposal slot, participation route, and boundaries |
| Daily/weekly tasks | Quests + CMI CustomText | `/daily`, `/quests list`, `/quests take <任务名>` | Gives players a repeatable entry point without becoming contribution scoring |
| Guild identity | BetterTeams + CMI CustomText | `/guild`, `/team create`, `/team join`, `/team chat` | Lets players form light guilds/teams for social identity and project records |
| Proposal intake | CMI CustomText | `/proposal` | Gives guilds a consistent project proposal template |
| Reward boundary | CMI CustomText | `/projectrewards` | Makes no-flight/no-gear/no-admin rewards explicit in game |
| Submission template | CMI CustomText | `/projectsubmit` | Helps project leads submit names, evidence, and reward drafts |

Plugin boundary:

- BetterTeams is configured for identity, team chat, and membership only. Team teleport, team bank, team chest claims, allies, PvP toggles, rankup, score rewards, and scoreboard team integration are disabled or permission-blocked.
- Quests is configured with three low-risk tasks: daily project check-in, weekly project wall support, and weekly redstone material support. These tasks give completion messages only and do not automatically grant contribution, gear, points, money, flight, or permissions.
- CMI flight charge is disabled for this activity model. The menu no longer exposes flight charge, `/flyc`, or fly speed entries to default players.

---

## Phase Plan

### Phase 0. Preflight Setup

Target date: before publishing week 1 projects.

| Task | Owner | Deliverable | Done When |
| --- | --- | --- | --- |
| Confirm the public project location | 管理 | Coordinates or route to the first project board | Players can reach it from spawn or main hub |
| Pick the week 1 official project owner | 管理 | Named owner for `W1-O1` | One person can answer questions and submit completion |
| Prepare the manual project record | 管理 | Copy of the project record template from the rules doc | It has fields for project, participants, evidence, rewards |
| Prepare player-facing copy | 管理 | Short announcement from the rules doc | It says rewards are recognition and light supplies only |
| Prepare reward packages | 管理 | 2-3 allowed light-supply bundles | None include flight, rare gear, admin powers, or high-value resources |
| Verify plugin entry commands | 管理 | `/projects`, `/daily`, `/guild`, `/proposal`, `/projectrewards` | A default player can open every in-game surface |
| Create pilot guilds | 玩家 / 管理 | BetterTeams team or temporary team names | Players can identify which group owns a project contribution |
| Run runtime smoke test | 管理 | `docs/operations/guild-projects/2026-06-09-runtime-smoke-test.md` | A default player can use project/task/guild commands and blocked features stay blocked |

Execution notes:

- Keep the reward package simple for the first week. One food/light bundle and one building bundle is enough.
- If temporary titles are used, treat them as a manual recognition item first. Do not build a title automation flow during the pilot.
- Quests task completion is not enough for project credit. The task output should point players back to负责人确认.

### Phase 1. Week 1 Launch

Target date: 2026-06-09.

| Task | Owner | Deliverable | Done When |
| --- | --- | --- | --- |
| Publish `W1-O1` | Official project owner | Announcement for Main Hub Project Board | Announcement includes goal, location, owner, deadline, rewards |
| Open guild proposal slot | Guild proposal reviewer | Proposal intake for `W1-G1` | Players know how to submit a proposal |
| Publish no-go reward boundary | 管理 | One short boundary note | Players see no神装, no飞行, no战力 |
| Direct players to contact path | 管理 | `找负责人` or `/helpop 内容` | Players know who to ask before starting |
| Point players to in-game commands | 管理 | `/projects`, `/daily`, `/guild` | Players can enter the loop without reading external docs |

Default if no proposal arrives:

- Use `W1-G1 Public Redstone Supply Pool`.
- Keep it small: at least 6 labeled material categories with starter stock.
- Require one named maintainer for weekly checks.

### Phase 2. Participation Window

Target dates: 2026-06-10 to 2026-06-14.

| Task | Owner | Deliverable | Done When |
| --- | --- | --- | --- |
| Track participant names | Project lead | Draft participant list | Each listed player has a visible contribution or accepted support role |
| Track project evidence | Project lead | Screenshots, coordinates, material notes, or visible build outcome | Management can verify without relying on memory |
| Keep public areas safe | Project lead + 管理 | No blocked roads, portals, villages, or private claims | No major location dispute remains open |
| Keep scope small | Project lead | Work stays within published project target | No project expands into an unreviewed mega-build |
| Handle disputes early | 管理 | Decision or补证 request | Disputes are not left until reward day |

Operating rules:

- A player who only idles online does not receive project participation.
- A player who contributes materials, testing, labeling, building, or coordination can be counted if the project lead records the contribution.
- Material donation is not a raw amount contest. Do not reward players for dumping the most items.
- If a contribution is unclear, ask for evidence rather than arguing in public chat.

### Phase 3. Submission and Review

Target dates: 2026-06-14 to 2026-06-15.

| Task | Owner | Deliverable | Done When |
| --- | --- | --- | --- |
| Submit project record | Project lead | Filled project record template | It includes participants, evidence, completion summary |
| Review public value | 管理 | Pass, partial pass, or reject | The project benefits public construction, resources, redstone, building, new players, or order |
| Review location safety | 管理 | Safety decision | It does not occupy private claims or public routes improperly |
| Review reward boundary | 管理 | Approved reward list | Rewards are recognition, display, records, or light supplies |
| Update displays | 管理 | Project wall or announcement update | Completed project and participant names are visible |

Decision rules:

- **Pass:** Count the project, update display, issue allowed rewards.
- **Partial pass:** Record completed parts and require补齐 before full guild reward.
- **Reject:** Do not count it as a weekly guild project; it can remain a private project.

### Phase 4. Week 1 Retrospective

Target date: 2026-06-16.

| Task | Owner | Deliverable | Done When |
| --- | --- | --- | --- |
| Count participation | 管理 | Simple pilot metrics | Unique participants, teams, project completions are recorded |
| Review management load | 管理 | Time and friction notes | Management knows whether the process is too heavy |
| Review reward safety | 管理 | Reward impact notes | No reward created combat, flight, economy, or permission problems |
| Decide week 2 adjustment | 管理 | Keep, shrink, or expand decision | Week 2 has a clear operating change or continuation |

Retrospective questions:

- Did players understand where to find projects through `/projects` or the `/menu` item?
- Did at least one project produce visible server value?
- Did the manual review take less than one management session?
- Did any reward feel too strong or too weak?
- Did public recognition motivate people more than item rewards?

---

## Week 1 Deliverables

| Deliverable | Source | Owner | Required |
| --- | --- | --- | --- |
| Project board or announcement area | `W1-O1` | Official project owner | Yes |
| Guild proposal slot or default redstone project | `W1-G1` | Guild proposal reviewer | Yes |
| Manual project records | Rules doc template | Project lead | Yes |
| Participation list | Project lead records | Project lead | Yes |
| Reward and display update | Review checklist | 管理 | Yes |
| Pilot retrospective notes | This plan | 管理 | Yes |

---

## Reward Execution Plan

Use rewards in this order:

1. **Record:** Add the player and guild to the project record.
2. **Display:** Put the project, guild, and participants on the project wall or announcement surface.
3. **Recognition:** Give temporary title候选 or visible署名 where practical.
4. **Light supply:** Give one small bundle from the allowed reward table.
5. **Future privilege:** For strong project leads, grant next-week proposal priority only.

Allowed first-week bundles:

| Bundle | Contents | Use Case |
| --- | --- | --- |
| Builder bundle | 1-3 stacks ordinary building blocks + 32 torches | Project wall, road, hub work |
| Runner bundle | 16-32 food + 8-16 normal fireworks | Travel and participation feedback |
| Maintainer bundle | 8-16 XP bottles +纪念书 or命名纸 | Redstone test, maintenance, project lead |

Reject any reward request that includes:

- Flight or flight charge.
- Diamond or netherite gear.
- High-level enchanted tools.
- WorldEdit, creative, admin, CoreProtect, or forced teleport permissions.
- Large quantities of iron, diamond, emerald, netherite, or other economy-shaping resources.

---

## Communication Plan

### Player Announcement

Use the player-facing short copy from `docs/brainstorms/2026-06-09-guild-project-daily-activity-rules.md`.

Add only the current project location and owner:

```text
本周公会项目已开放。
项目墙位置：<坐标或路线>
负责人：<玩家名>

每周 1 个官方项目 + 1 个公会提案项目。
奖励只给称号、署名、展示和少量补给，不给神装、不送飞行、不加战力。

想报名：找项目负责人，或用 /helpop 内容 联系管理。
```

### Management Notes

- Repeat the boundary once at launch, not every login.
- Point confused players to `/menu` and `/rules` only if those surfaces already contain useful context.
- Do not over-explain the whole system in chat. The first project should teach the loop by being visible.

---

## Metrics

Track these manually for week 1:

| Metric | Target | Why It Matters |
| --- | --- | --- |
| Unique participants | 4+ | Shows the loop is more than a solo task |
| Teams or guilds involved | 1+ | Tests the group identity premise |
| Completed public projects | 1+ | Proves visible server value |
| Management review time | Under 30 minutes per project | Keeps the system maintainable |
| Reward disputes | 0-1 | Measures whether reward rules are clear |
| Boundary violations | 0 | Confirms no combat, flight, or permission creep |

If participation is low but feedback is positive, repeat with simpler projects before adding automation. If management review is too heavy, reduce week 2 to one official project only.

---

## Risks and Mitigations

| Risk | Signal | Mitigation |
| --- | --- | --- |
| Players treat projects as item reward farming | Questions focus on rewards, not public outcome | Emphasize署名 and project record; keep supply bundles small |
| Private builds are submitted as public projects | Proposal mainly benefits one base | Reject or reclassify as private project |
| Manual review becomes too much work | Review takes more than one session | Reduce to one project per week or require clearer project lead records |
| Reward creep starts | Requests for flight, gear, tools, or rare resources | Reject immediately and point to the no-go list |
| Public area conflict | Project blocks roads, portals, claims, or villages | Require location approval before work starts |
| Players do not see the system | Questions repeat after launch | Move project wall closer to spawn, keep `/projects` in `/menu`, and repeat the short `/rules` route |
| Plugin task completion is mistaken for contribution | Players ask for rewards after only doing `/daily` | Repeat that Quests is only an entry/task prompt; contribution requires负责人名单 and管理验收 |

---

## Week 2 Decision Gate

At the end of week 1, choose one path:

| Condition | Week 2 Move |
| --- | --- |
| Good participation, low management load | Continue with 1 official + 1 proposal project |
| Good participation, high management load | Keep projects but require stronger project lead records |
| Low participation, low confusion | Use easier projects and stronger in-game visibility |
| Low participation, high confusion | Pause proposal slot and run one official tutorial-style project |
| Reward disputes or boundary pressure | Tighten reward language before any new project |

Do not add stronger plugin automation, point shops, automatic guild contribution, or extra reward mechanics until the manual loop produces at least two successful weekly cycles.

---

## References

- `docs/brainstorms/2026-06-09-guild-project-daily-activity-requirements.md`
- `docs/brainstorms/2026-06-09-guild-project-daily-activity-rules.md`
- `docs/operations/guild-projects/2026-06-09-runtime-smoke-test.md`
- `README.md`
- `docs/research/2026-06-09-guild-project-plugin-selection.md`
- `plugins/TAB/groups.yml`
- `plugins/LuckPerms/yaml-storage/groups/default.yml`
- `plugins/CommandGUI/config.yml`
