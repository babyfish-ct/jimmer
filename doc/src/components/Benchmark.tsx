import React, { useCallback, useState } from "react";
import {
	G2,
	Chart,
	Tooltip,
	Interval,
	Interaction,
    Axis,
    Legend
} from "bizcharts";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Divider from '@mui/material/Divider';
import { ScaleOption } from "bizcharts/lib/interface";

export const Benchmark: React.FC<{
    readonly type: "OPS" | "TIME",
    readonly locale?: "zh"
}> = ({type, locale}) => {

    const [showJdbc, setShowJdbc] = useState(type !== "OPS");
    const onShowJdbcChange = useCallback((event: React.ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setShowJdbc(checked);
    }, []);
    return (
        <Tabs>
            <TabItem value="chart" label={locale === "zh" ? "图表" : "Chart"} default>
                { 
                    type === "OPS" && 
                    <>
                        <FormGroup>
                            <FormControlLabel 
                            control={<Checkbox value={showJdbc} onChange={onShowJdbcChange}/>} 
                            label={locale === "zh" ? "显示原生JDBC指标" : "Show native JDBC metrics"}/>
                        </FormGroup> 
                        <Divider/>
                    </>
                }
                {
                    type == "OPS" ?
                    <BenchmarkChart rows={opsRows} scale={opsScale} showJdbc={showJdbc}/> :
                    <BenchmarkChart rows={timeRows} scale={timeScale} showJdbc={showJdbc}/>
                }
            </TabItem>
            <TabItem value="data" label={locale === "zh" ? "数据" : "Data"}>
                {
                    type === "OPS" ?
                    <BenchmarkTable rows={opsRows} valueTitle="Ops/s"/> :
                    <BenchmarkTable rows={timeRows} valueTitle="Time(μs)"/>
                }
            </TabItem>
        </Tabs>
    );
};

const BenchmarkChart: React.FC<{
    readonly rows: ReadonlyArray<Row>,
    readonly scale: {
        readonly [field: string]: ScaleOption
    },
    readonly showJdbc: boolean
}> = ({rows, scale, showJdbc}) => {
    return (
        <Chart 
        filter={showJdbc ? undefined : {"framework": (v: any) => !(v as string).startsWith("JDBC")}}
        height={400} 
        padding="auto" 
        data={rows} 
        scale={scale} 
        autoFit>
			<Interval
				adjust={[
					{
						type: 'dodge',
						marginRatio: 0,
					},
				]}
				color="framework"
				position="dataCount*value"
			/>
            <Axis name="value" title/>
            <Legend position="left"/>
			<Tooltip shared />
			<Interaction type="active-region" />
		</Chart>
    );
}

const BenchmarkTable: React.FC<{readonly rows: ReadonlyArray<Row>, readonly valueTitle: string}> = ({rows, valueTitle}) => {
    return (
        <Table>
            <TableHead>
                <TableRow>
                    <TableCell>Framework</TableCell>
                    <TableCell>Data count</TableCell>
                    <TableCell>{valueTitle}</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {
                    rows.map(row => 
                        <TableRow key={`${row.framework}-${row.dataCount}`}>
                            <TableCell>{row.framework}</TableCell>
                            <TableCell>{row.dataCount}</TableCell>
                            <TableCell>{row.value}</TableCell>
                        </TableRow>
                    )
                }
            </TableBody>
        </Table>
    );
};

const opsScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: "Ops/s"
	}
};

const timeScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: 'Time(μs)'
	}
};

interface Row {
    readonly framework: string;
    readonly dataCount: string;
    readonly value: number;
};

const rows: ReadonlyArray<Row> = [
	{framework: "JDBC(ColIndex)", dataCount: "10", value: 694486.3419038279},
    {framework: "JDBC(ColIndex)", dataCount: "20", value: 489680.5448583284},
    {framework: "JDBC(ColIndex)", dataCount: "50", value: 264206.57460154407},
    {framework: "JDBC(ColIndex)", dataCount: "100", value: 150515.12108999112},
    {framework: "JDBC(ColIndex)", dataCount: "200", value: 79814.62925392622},
    {framework: "JDBC(ColIndex)", dataCount: "500", value: 34010.917810255225},
    {framework: "JDBC(ColIndex)", dataCount: "1000", value: 17344.997133330337},
    {framework: "JDBC(ColName)", dataCount: "10", value: 344937.67467185546},
    {framework: "JDBC(ColName)", dataCount: "20", value: 255724.33711667656},
    {framework: "JDBC(ColName)", dataCount: "50", value: 136098.32519053208},
    {framework: "JDBC(ColName)", dataCount: "100", value: 73348.19440278952},
    {framework: "JDBC(ColName)", dataCount: "200", value: 41339.312973684755},
    {framework: "JDBC(ColName)", dataCount: "500", value: 17442.958296769142},
    {framework: "JDBC(ColName)", dataCount: "1000", value: 8851.357551662291},
    {framework: "Jimmer", dataCount: "10", value: 205436.22983583916},
    {framework: "Jimmer", dataCount: "20", value: 106671.0555555964},
    {framework: "Jimmer", dataCount: "50", value: 47416.202602896825},
    {framework: "Jimmer", dataCount: "100", value: 24356.49940496951},
    {framework: "Jimmer", dataCount: "200", value: 12446.816603773445},
    {framework: "Jimmer", dataCount: "500", value: 5043.100504924187},
    {framework: "Jimmer", dataCount: "1000", value: 2591.485543202771},
    {framework: "MyBatis", dataCount: "10", value: 76883.8077420863},
    {framework: "MyBatis", dataCount: "20", value: 43902.97281806945},
    {framework: "MyBatis", dataCount: "50", value: 19651.20228086318},
    {framework: "MyBatis", dataCount: "100", value: 10282.527318386752},
    {framework: "MyBatis", dataCount: "200", value: 5077.795477849398},
    {framework: "MyBatis", dataCount: "500", value: 2183.073394058281},
    {framework: "MyBatis", dataCount: "1000", value: 1077.022064385448},
    {framework: "JPA(Hibernate)", dataCount: "10", value: 102079.99683590114},
    {framework: "JPA(Hibernate)", dataCount: "20", value: 59571.8912591726},
    {framework: "JPA(Hibernate)", dataCount: "50", value: 25702.786559093773},
    {framework: "JPA(Hibernate)", dataCount: "100", value: 13414.569591700454},
    {framework: "JPA(Hibernate)", dataCount: "200", value: 6950.7789429148015},
    {framework: "JPA(Hibernate)", dataCount: "500", value: 2767.8983414626673},
    {framework: "JPA(Hibernate)", dataCount: "1000", value: 1394.4277761346852},
    {framework: "JPA(EclipseLink)", dataCount: "10", value: 64196.05849313359},
    {framework: "JPA(EclipseLink)", dataCount: "20", value: 33404.26805171479},
    {framework: "JPA(EclipseLink)", dataCount: "50", value: 13366.495947592284},
    {framework: "JPA(EclipseLink)", dataCount: "100", value: 6463.698802395989},
    {framework: "JPA(EclipseLink)", dataCount: "200", value: 3372.576544558393},
    {framework: "JPA(EclipseLink)", dataCount: "500", value: 1336.372383001008},
    {framework: "JPA(EclipseLink)", dataCount: "1000", value: 633.8655654287536},
    {framework: "JOOQ", dataCount: "10", value: 57330.45419894458},
    {framework: "JOOQ", dataCount: "20", value: 32028.461417183466},
    {framework: "JOOQ", dataCount: "50", value: 14314.38547680882},
    {framework: "JOOQ", dataCount: "100", value: 7502.648309980643},
    {framework: "JOOQ", dataCount: "200", value: 3763.6724215854615},
    {framework: "JOOQ", dataCount: "500", value: 1526.2019510344485},
    {framework: "JOOQ", dataCount: "1000", value: 819.9420644410151},
    {framework: "Exposed", dataCount: "10", value: 90907.0038077874},
    {framework: "Exposed", dataCount: "20", value: 56193.792591162506},
    {framework: "Exposed", dataCount: "50", value: 27223.72463486148},
    {framework: "Exposed", dataCount: "100", value: 16082.303029792809},
    {framework: "Exposed", dataCount: "200", value: 8729.672525241589},
    {framework: "Exposed", dataCount: "500", value: 3339.056688942882},
    {framework: "Exposed", dataCount: "1000", value: 1706.3058644786568},
    {framework: "Nutz", dataCount: "10", value: 83494.05338964531},
    {framework: "Nutz", dataCount: "20", value: 40717.14800799643},
    {framework: "Nutz", dataCount: "50", value: 17614.258653514986},
    {framework: "Nutz", dataCount: "100", value: 8964.770360354232},
    {framework: "Nutz", dataCount: "200", value: 5027.467397793748},
    {framework: "Nutz", dataCount: "500", value: 1752.6091274012165},
    {framework: "Nutz", dataCount: "1000", value: 907.6214231393163},
    {framework: "ObjectiveSQL", dataCount: "10", value: 59785.4698873157},
    {framework: "ObjectiveSQL", dataCount: "20", value: 31686.481503294886},
    {framework: "ObjectiveSQL", dataCount: "50", value: 12956.668448069668},
    {framework: "ObjectiveSQL", dataCount: "100", value: 6533.652114072017},
    {framework: "ObjectiveSQL", dataCount: "200", value: 2962.704281294185},
    {framework: "ObjectiveSQL", dataCount: "500", value: 1368.3867839343445},
    {framework: "ObjectiveSQL", dataCount: "1000", value: 595.9637213597805},
    {framework: "Spring data jdbc", dataCount: "10", value: 19928.66836787829},
    {framework: "Spring data jdbc", dataCount: "20", value: 10393.084996776615},
    {framework: "Spring data jdbc", dataCount: "50", value: 4066.7112309312906},
    {framework: "Spring data jdbc", dataCount: "100", value: 2129.3697205181825},
    {framework: "Spring data jdbc", dataCount: "200", value: 1035.8068663111264},
    {framework: "Spring data jdbc", dataCount: "500", value: 429.52774464126685},
    {framework: "Spring data jdbc", dataCount: "1000", value: 195.7003347507818},
    {framework: "Ktorm", dataCount: "10", value: 16307.248701814044},
    {framework: "Ktorm", dataCount: "20", value: 8927.241426690754},
    {framework: "Ktorm", dataCount: "50", value: 4071.264434393904},
    {framework: "Ktorm", dataCount: "100", value: 2096.8860331106425},
    {framework: "Ktorm", dataCount: "200", value: 1057.9144168099972},
    {framework: "Ktorm", dataCount: "500", value: 374.82800208842036},
    {framework: "Ktorm", dataCount: "1000", value: 176.26688013436913}
];

const opsRows = rows.map(item => ({...item, value: Math.round(item.value)}));

const timeRows = rows.map(item => ({...item, value: Math.round(1000000 / item.value)}));
