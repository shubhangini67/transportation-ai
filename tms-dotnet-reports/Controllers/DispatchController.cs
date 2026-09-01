using Microsoft.AspNetCore.Mvc;
using TmsReports.Models;
using TmsReports.Services;

namespace TmsReports.Controllers;

[ApiController]
[Route("api/v1/dispatch")]
public class DispatchController : ControllerBase
{
    private readonly DispatchOptimizer _optimizer;

    public DispatchController(DispatchOptimizer optimizer)
    {
        _optimizer = optimizer;
    }

    [HttpPost("recommend")]
    public ActionResult<DispatchPlanResponse> Recommend([FromBody] DispatchSnapshotRequest snapshot)
    {
        if (snapshot is null)
        {
            return BadRequest(new { error = "Snapshot body is required" });
        }

        return Ok(_optimizer.Recommend(snapshot));
    }
}
