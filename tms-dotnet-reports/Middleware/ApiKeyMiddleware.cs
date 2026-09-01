namespace TmsReports.Middleware;

/// <summary>
/// Service-to-service auth. The React app never calls this API directly —
/// Spring Boot sends X-Internal-Api-Key on each request.
/// </summary>
public class ApiKeyMiddleware
{
    private const string HeaderName = "X-Internal-Api-Key";
    private readonly RequestDelegate _next;
    private readonly string _expectedKey;

    public ApiKeyMiddleware(RequestDelegate next, IConfiguration config)
    {
        _next = next;
        _expectedKey = config["InternalApiKey"] ?? "tms-internal-dev-key";
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var path = context.Request.Path.Value ?? "";
        if (path.Equals("/health", StringComparison.OrdinalIgnoreCase))
        {
            await _next(context);
            return;
        }

        if (!context.Request.Headers.TryGetValue(HeaderName, out var provided)
            || !string.Equals(provided.ToString(), _expectedKey, StringComparison.Ordinal))
        {
            context.Response.StatusCode = StatusCodes.Status401Unauthorized;
            await context.Response.WriteAsJsonAsync(new { error = "Invalid or missing internal API key" });
            return;
        }

        await _next(context);
    }
}
