package mcp

default allow := true

#NSECS_PER_DAY := 86400000000000  # 24*60*60*1e9
#
#date_to_ns(d) := ns if {
#  is_string(d)
#  ns := time.parse_rfc3339_ns(sprintf("%sT00:00:00Z", [d]))
#}
#
#days_between(from, to) := diff_days if {
#  from_ns := date_to_ns(from)
#  to_ns   := date_to_ns(to)
#  diff_days := floor((to_ns - from_ns) / NSECS_PER_DAY)
#}
#
#too_wide_window if {
#  input.args.from
#  input.args.to
#  days_between(input.args.from, input.args.to) > 31
#}
#
## ---- POC: treat missing principal as OK ----
#principal_ok if { true }  # always true for POC; tighten later
#
## Allow exportOrders (≤ 31 days)
#allow if {
#  input.tool == "exportOrders"
#  principal_ok
#  not too_wide_window
#}
#
## Allow getCustomerByEmail
#allow if {
#  input.tool == "getCustomerByEmail"
#  principal_ok
#}
