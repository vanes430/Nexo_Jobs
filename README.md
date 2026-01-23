# NexoJobs - Skript Integration 


## Available Placeholders

### General Job Information

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%nexojobs_job%` | Current active job name | `MINER` |
| `%nexojobs_job_id%` | Current active job ID | `miner` |
| `%nexojobs_active_count%` | Number of active jobs | `2` |
| `%nexojobs_job_limit%` | Maximum jobs allowed | `5` or `Unlimited` |
| `%nexojobs_has_job%` | Has any active job | `true` or `false` |
| `%nexojobs_status%` | Current job status | `ACTIVE` |

### Current Active Job Stats

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%nexojobs_level%` | Current job level | `25` |
| `%nexojobs_exp%` | Current job EXP | `1500` |
| `%nexojobs_exp_required%` | EXP needed for level | `2000` |
| `%nexojobs_max_level%` | Maximum level | `50` |
| `%nexojobs_percentage%` | Progress percentage | `75.0` |
| `%nexojobs_exp_remaining%` | EXP to next level | `500` |
| `%nexojobs_progress_bar%` | Visual progress bar | `▰▰▰▰▰▰▰▱▱▱` |

### Specific Job Stats
Replace `<jobid>` with: `miner`, `farmer`, `hunter`, `lumberjack`, `enchanter`, `alchemist`, `blacksmith`, `digger`, `chef`, `murderer`

| Placeholder | Description | Example |
|------------|-------------|---------|
| `%nexojobs_<jobid>_level%` | Job level | `%nexojobs_miner_level%` → `30` |
| `%nexojobs_<jobid>_exp%` | Job EXP | `%nexojobs_farmer_exp%` → `5000` |
| `%nexojobs_<jobid>_required%` | EXP required | `%nexojobs_hunter_required%` → `8000` |
| `%nexojobs_<jobid>_percentage%` | Progress % | `%nexojobs_miner_percentage%` → `62.5` |
| `%nexojobs_<jobid>_max_level%` | Max level | `%nexojobs_farmer_max_level%` → `50` |
| `%nexojobs_<jobid>_status%` | Job status | `%nexojobs_miner_status%` → `ACTIVE` |
| `%nexojobs_<jobid>_is_active%` | Is active | `%nexojobs_miner_is_active%` → `true` |
| `%nexojobs_<jobid>_has_started%` | Has progress | `%nexojobs_farmer_has_started%` → `true` |

## Common Use Cases

### 1. Level Requirement Check
```skript
command /testjobspapi:
    trigger:
        set {_minerLevel} to placeholder "nexojobs_miner_level" from player parsed as integer
        set {_farmerLevel} to placeholder "nexojobs_farmer_level" from player parsed as integer
        
        if {_minerLevel} >= 25:
            send "&aYou have access, because Miner level 25+!" to player
        else if {_farmerLevel} >= 30:
            send "&aYou have access, because Farmer level 30+!" to player
        else:
            send "&cYou need Miner level 25+ or Farmer level 30+ for VIP!" to player
```

### 2. Multiple Job Requirement
```skript
command /shop:
    trigger:
        set {_miner} to placeholder "nexojobs_miner_level" from player parsed as integer
        set {_blacksmith} to placeholder "nexojobs_blacksmith_level" from player parsed as integer
        
        if {_miner} >= 30:
            if {_blacksmith} >= 30:
                send "&aWelcome to Shop!" to player
            else:
                send "&cYou need Blacksmith level 30+ too!" to player
        else:
            send "&cYou need Miner level 30+ and Blacksmith level 30+!" to player
```


