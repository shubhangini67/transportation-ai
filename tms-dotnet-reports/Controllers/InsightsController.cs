using Microsoft.AspNetCore.Mvc;
using TmsReports.Models;
using TmsReports.Services;

namespace TmsReports.Controllers;

[ApiController]
[Route("api/v1/insights")]
public class InsightsController : ControllerBase
{
    private readonly FleetInsightEngine _engine;

    public InsightsController(FleetInsightEngine engine)
    {
        _engine = engine;
    }

    [HttpPost("fleet")]
    public ActionResult<FleetInsightsResponse> Fleet([FromBody] FleetSnapshotRequest snapshot)
    {
        if (snapshot is null)
        {
            return BadRequest(new { error = "Snapshot body is required" });
        }

        return Ok(_engine.Analyze(snapshot));
    }
}
