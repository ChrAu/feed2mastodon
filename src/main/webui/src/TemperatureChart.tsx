import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import moment from 'moment';

const rawData = [
  { last_changed: '2025-09-12 11:15:34.592595 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-12 11:00:30.766247 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-12 10:44:00.635998 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-12 10:13:56.585037 +02:00', current_temperature: '22.0' },
  { last_changed: '2025-09-12 09:13:54.585606 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-12 07:28:36.623125 +02:00', current_temperature: '22.0' },
  { last_changed: '2025-09-12 07:01:34.576725 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-12 06:28:55.621701 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-12 05:58:48.475720 +02:00', current_temperature: '22.0' },
  { last_changed: '2025-09-12 05:43:46.628594 +02:00', current_temperature: '21.5' },
  { last_changed: '2025-09-12 05:28:46.621338 +02:00', current_temperature: '21.0' },
  { last_changed: '2025-09-12 05:01:34.463122 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 04:42:01.560207 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 04:41:12.427637 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 04:26:01.480158 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 04:25:12.363631 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 04:04:01.522162 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 04:03:12.064243 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 02:43:44.623972 +02:00', current_temperature: '20.5' },
  { last_changed: '2025-09-12 01:43:14.642101 +02:00', current_temperature: '21.0' },
  { last_changed: '2025-09-12 00:28:13.600367 +02:00', current_temperature: '21.5' },
  { last_changed: '2025-09-12 00:02:11.639102 +02:00', current_temperature: '22.0' },
  { last_changed: '2025-09-11 23:01:01.507635 +02:00', current_temperature: '22.0' },
  { last_changed: '2025-09-11 22:13:34.598172 +02:00', current_temperature: '22.0' },
  { last_changed: '2025-09-11 22:09:34.235346 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-11 22:09:12.282088 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-11 22:01:34.285500 +02:00', current_temperature: '22.5' },
  { last_changed: '2025-09-11 13:28:10.734654 +02:00', current_temperature: '22.5' }
];

const processedData = rawData.map(d => ({
  timestamp: new Date(d.last_changed).getTime(),
  temperature: parseFloat(d.current_temperature)
})).reverse(); // Daten sortieren, da die API in umgekehrter Reihenfolge liefert

const dateFormatter = (timestamp: number) => {
  return moment(timestamp).format('HH:mm');
};

const TemperatureChart = () => {
  return (
    <div style={{ padding: '20px', backgroundColor: '#f9f9f9', borderRadius: '10px' }}>
      <h2 style={{ textAlign: 'center', color: '#333' }}>Temperaturverlauf</h2>
      <ResponsiveContainer width="100%" height={400}>
        <LineChart data={processedData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="timestamp"
            scale="time"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={dateFormatter}
            minTickGap={20}
          />
          <YAxis dataKey="temperature" />
          <Tooltip
            labelFormatter={(label) => moment(label).format('DD.MM.YY, HH:mm')}
            formatter={(value) => [`${value} °C`, 'Temperatur']}
          />
          <Legend />
          <Line
            type="stepAfter" // Hier ist die wichtige Änderung
            dataKey="temperature"
            stroke="#8884d8"
            strokeWidth={2}
            dot={false}
            // dot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TemperatureChart;
