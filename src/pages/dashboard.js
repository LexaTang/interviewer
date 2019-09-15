import { Component } from 'react';
import { Layout, Typography, Row, Col, Card, Form, Slider, Input, Button, List, Modal } from 'antd';

import { connect } from 'dva';
import styles from './dashboard.less';

const { Header } = Layout;
const { Title } = Typography;

class EnqueueForm extends Component {
  submitHandler = e => {
    const { form, resultHandler } = this.props;

    e.preventDefault();
    form.validateFields((err, values) => {
      if (!err) {
        resultHandler(values);
        form.resetFields();
      }
    });
  };

  render() {
    const { form } = this.props;
    return (
      <Form onSubmit={this.submitHandler}>
        <Form.Item label="面试者">
          {form.getFieldDecorator('interviewee', {
            rules: [{ required: true, message: '请输入面试者！' }],
          })(<Input />)}
        </Form.Item>
        <Form.Item label="房间号">{form.getFieldDecorator('room')(<Input />)}</Form.Item>
        <Button type="primary" htmlType="submit">
          提交
        </Button>
      </Form>
    );
  }
}
const EnqueueComponet = Form.create()(EnqueueForm);

@connect(({ loading, queue }) => ({
  loading,
  queue: queue.queue,
  vip: queue.vip,
  rooms: queue.rooms,
}))
class Dashboard extends Component {
  constructor(props) {
    super(props);
    this.state = {
      infoVisible: false,
    };
  }

  componentDidMount() {
    const { dispatch } = this.props;
    window.setInterval(() => dispatch({ type: 'queue/fetch' }), 1000);
  }

  render() {
    const { rooms, vip, queue, info, dispatch } = this.props;

    const resultHandler = ({interviewee, room}) => {
      if (room === "" || room === undefined) dispatch({ type: 'queue/enqueue', id: interviewee});
      else dispatch({ type: 'queue/envip', id: interviewee, room});
    };

    return (
      <div>
        <Header>
          <Title className="title">面试控制台</Title>
        </Header>
        <br />
        <Row gutter={16}>
          <Col span={3} />
          <Col span={7}>
            <EnqueueComponet resultHandler={resultHandler} />
            <br />
            <List
              header={<div>房间状态</div>}
              bordered
              dataSource={rooms}
              renderItem={item => (
                <List.Item>
                  <Typography.Text underline>{item.id}</Typography.Text> <Typography.Text mark>{item.interviewing}</Typography.Text> {item.next}
                </List.Item>
              )}
            />
          </Col>
          <Col span={4}>
            <List
              header={<div>推送队列</div>}
              bordered
              dataSource={vip}
              renderItem={item => (
                <List.Item>
                  <Typography.Text mark>{Object.keys(item)[0]}</Typography.Text> {item[Object.keys(item)[0]]}
                </List.Item>
              )}
            />
          </Col>
          <Col span={6}>
            <List
              header={<div>全局队列</div>}
              bordered
              dataSource={queue}
              renderItem={item => <List.Item>{item}</List.Item>}
            />
          </Col>
        </Row>
        <Modal
          title="用户信息"
          visible={this.state.infoVisible}
          onOk={() => this.setState({ infoVisible: false })}
          onCancel={() => this.setState({ infoVisible: false })}
        >
          {info && Object.keys(info).length ? info : null}
        </Modal>
      </div>
    );
  }
}

export default Dashboard;