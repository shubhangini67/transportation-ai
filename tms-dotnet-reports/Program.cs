using System.Text.Json;
using TmsReports.Middleware;
using TmsReports.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.DictionaryKeyPolicy = JsonNamingPolicy.CamelCase;
    });
builder.Services.AddHealthChecks();
builder.Services.AddSingleton<FleetInsightEngine>();
builder.Services.AddSingleton<DispatchOptimizer>();

var app = builder.Build();

app.MapHealthChecks("/health");
app.UseMiddleware<ApiKeyMiddleware>();
app.MapControllers();

app.Run();
